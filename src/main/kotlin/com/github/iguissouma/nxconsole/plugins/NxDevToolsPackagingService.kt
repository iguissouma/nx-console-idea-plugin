package com.github.iguissouma.nxconsole.plugins

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.util.NxExecutionUtil
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.packages.NodePackageInfo
import com.intellij.javascript.nodejs.packages.NodePackageInfoException
import com.intellij.javascript.nodejs.settings.NodeInstalledPackage
import com.intellij.javascript.nodejs.settings.NodeInstalledPackagesProvider
import com.intellij.javascript.nodejs.settings.NodePackageInfoManager
import com.intellij.javascript.nodejs.settings.NodePackageManagementService
import com.intellij.lang.javascript.buildTools.npm.rc.NpmCommand
import com.intellij.lang.javascript.modules.PackageInstaller
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.CatchingConsumer
import com.intellij.util.ObjectUtils
import com.intellij.webcore.packaging.InstalledPackage
import com.intellij.webcore.packaging.PackageManagementServiceEx
import com.intellij.webcore.packaging.RepoPackage
import java.io.File
import java.util.Arrays

fun getPackageList(nxListOutput: String, from: String, to: String, trim: String): List<String> {
    val i = nxListOutput.indexOf(from) + from.length
    return nxListOutput.substring(i, nxListOutput.indexOf(to))
        .split("\n")
        .map { it.trim() }
        .filterNot { it.isEmpty() }
        .map { it.substring(0, it.indexOf(trim)).trim() }
        .toList()
}

private fun getInstalled(command: String) = getPackageList(command, ">  NX   Installed plugins:", ">  NX   Also available:", " (")
private fun getAlsoAvailable(command: String) = getPackageList(command, ">  NX   Also available:", ">  NX   Community plugins:", " (")
private fun getCommunityPlugins(command: String) = getPackageList(
    command,
    ">  NX   Community plugins:",
    ">  NX   Use \"nx list [plugin]\" to find out more",
    " -"
)

class NxDevToolsPackagingService(
    val myProject: Project,
    val mySettings: NxDevToolsSettings,
    val myInterpreter: NodeJsInterpreter
) : PackageManagementServiceEx() {

    private val myManager = NodePackageInfoManager(myProject, myInterpreter)

    private val LOG = Logger.getInstance(
        NodePackageManagementService::class.java
    )

    fun getSettings(): NxDevToolsSettings {
        return this.mySettings
    }

    override fun getAllPackages(): MutableList<RepoPackage> {
        val output = NxExecutionUtil(myProject).executeAndGetOutput("list") ?: return mutableListOf()
        if (output.exitCode != 0) {
            return mutableListOf()
        }
        val commandResult = output.stdout
        return (getInstalled(commandResult) + getAlsoAvailable(commandResult) + getCommunityPlugins(commandResult))
            .map { RepoPackage(it, "", "") }
            .toMutableList()
    }

    override fun reloadAllPackages(): MutableList<RepoPackage> {
        return this.allPackages
    }

    override fun getInstalledPackages(): MutableCollection<InstalledPackage> {
        val output = NxExecutionUtil(myProject).executeAndGetOutput("list") ?: return mutableListOf()
        if (output.exitCode != 0) {
            return mutableListOf()
        }
        val commandResult = output.stdout
        val installed = getInstalled(commandResult)
        return if (myProject.isDisposed) {
            mutableListOf()
        } else {
            val provider = NodeInstalledPackagesProvider.getInstance()
            val dir = myProject.baseDir
            val ioDir = if (dir != null && dir.isValid && dir.isDirectory) VfsUtilCore.virtualToIoFile(dir) else null
            val nodePackages = provider.listInstalledPackages(ioDir, NodeJsLocalInterpreter.tryCast(this.myInterpreter))
            val array = nodePackages.toTypedArray() as Array<NodeInstalledPackage>
            Arrays.sort(
                array
            ) { p1: NodeInstalledPackage, p2: NodeInstalledPackage ->
                if (p1.isGlobal != p2.isGlobal) {
                    return@sort if (p1.isGlobal) 1 else -1
                } else {
                    return@sort p1.name.compareTo(p2.name)
                }
            }
            mutableListOf(*array).filter { installed.contains(it.name) }.toMutableList()
        }
    }

    override fun installPackage(
        repoPackage: RepoPackage?,
        version: String?,
        forceUpgrade: Boolean,
        extraOptions: String?,
        listener: Listener?,
        installToUser: Boolean
    ) {
        val baseDir = myProject.baseDir
        if (baseDir != null) {
            ProgressManager.getInstance().run(
                PackageInstaller(
                    myProject,
                    myInterpreter,
                    repoPackage!!.name,
                    version,
                    VfsUtilCore.virtualToIoFile(baseDir),
                    listener!!,
                    extraOptions ?: "--save-dev --save-exact"
                )
            )
        }
    }

    override fun installPackage(
        repoPackage: RepoPackage,
        version: String?,
        extraOptions: String?,
        listener: Listener?,
        workingDir: File
    ) {
        ProgressManager.getInstance().run(
            PackageInstaller(
                myProject,
                myInterpreter,
                repoPackage.name,
                version,
                workingDir,
                listener!!,
                extraOptions
            )
        )
    }

    override fun uninstallPackages(installedPackages: MutableList<InstalledPackage>?, listener: Listener) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val var3: Iterator<*> = installedPackages!!.iterator()
            while (var3.hasNext()) {
                val installedPackage = var3.next() as InstalledPackage
                if (installedPackage is NodeInstalledPackage) {
                    this.uninstallPackage(installedPackage as NodeInstalledPackage, listener)
                }
            }
        }
    }

    private fun uninstallPackage(installedPackage: NodeInstalledPackage, listener: Listener) {
        val args: MutableList<String> = mutableListOf()
        if (installedPackage.isGlobal) {
            args.add("-g")
        }
        args.add(installedPackage.name)
        runNpmCommand(installedPackage.name, installedPackage, NpmCommand.UNINSTALL, args, listener)
    }

    override fun fetchPackageVersions(
        packageName: String,
        consumer: CatchingConsumer<MutableList<String>, Exception>?
    ) {
        this.myManager.fetchPackageInfo(
            object : NodePackageInfoManager.PackageInfoConsumer(packageName) {
                override fun onPackageInfo(packageInfo: NodePackageInfo?) {
                    if (packageInfo != null) {
                        consumer!!.consume(packageInfo.versions)
                    }
                }

                override fun onException(e: java.lang.Exception) {
                    consumer!!.consume(e)
                }
            }
        )
    }

    override fun fetchPackageDetails(packageName: String?, consumer: CatchingConsumer<String, Exception>?) {
        myManager.fetchPackageInfo(
            object : NodePackageInfoManager.PackageInfoConsumer(packageName!!) {
                override fun onPackageInfo(packageInfo: NodePackageInfo?) {
                    if (packageInfo != null) {
                        consumer!!.consume(packageInfo.formatHtmlDescription())
                    }
                }

                override fun onException(e: java.lang.Exception) {
                    if (e is NodePackageInfoException) {
                        consumer!!.consume(e.formatHtmlDescription())
                    }
                    consumer!!.consume(e)
                }
            }
        )
    }

    override fun getID(): String? {
        return "Nx DevTools"
    }

    override fun updatePackage(installedPackage: InstalledPackage, version: String?, listener: Listener) {
        val pkg = ObjectUtils.tryCast(
            installedPackage,
            NodeInstalledPackage::class.java
        )
        if (pkg != null) {
            ApplicationManager.getApplication().executeOnPooledThread {
                val arg = pkg.name + if (version != null) "@$version" else ""
                this.runNpmCommand(pkg.name, pkg, NpmCommand.ADD, listOf(arg), listener)
            }
        }
    }

    private fun runNpmCommand(
        packageName: String,
        pkg: NodeInstalledPackage?,
        command: NpmCommand,
        args: List<String>,
        listener: Listener
    ) {
        val workingDir: File? = this.guessWorkingDir(pkg)
        if (workingDir == null) {
            val message = NxBundle.message(
                "nx.node.packages.cannot_find_working_directory.text",
                packageName,
                args
            )
            LOG.warn(message)
            ApplicationManager.getApplication().invokeLater(
                {
                    listener.operationFinished(
                        packageName,
                        ErrorDescription.fromMessage(message)
                    )
                },
                ModalityState.any()
            )
        } else {
            val commandLine = PackageInstaller.computeAndReportIfFailed<GeneralCommandLine, ExecutionException>(
                listener,
                packageName
            ) {
                NpmUtil.createNpmCommandLine(
                    myProject,
                    workingDir,
                    myInterpreter,
                    command,
                    args
                )
            }
            if (commandLine != null) {
                val errorMessageRef: Ref<String> = Ref.create<String>()
                try {
                    ApplicationManager.getApplication()
                        .invokeLater({ listener.operationStarted(packageName) }, ModalityState.any())
                    val processHandler = CapturingProcessHandler(commandLine)
                    val output = processHandler.runProcess()
                    if (output.exitCode != 0) {
                        errorMessageRef.set(NodeCommandLineUtil.formatErrorMessage(commandLine, output))
                    }
                } catch (var14: ExecutionException) {
                    errorMessageRef.set(var14.message)
                } finally {
                    ApplicationManager.getApplication().invokeLater(
                        {
                            listener.operationFinished(
                                packageName,
                                ErrorDescription.fromMessage(errorMessageRef.get() as String)
                            )
                            LocalFileSystem.getInstance().refresh(true)
                        },
                        ModalityState.any()
                    )
                }
            }
        }
    }

    private fun guessWorkingDir(pkg: NodeInstalledPackage?): File? {
        if (pkg != null) {
            var workingDir = pkg.sourceRootDir.parentFile
            if (workingDir != null && workingDir.name == "node_modules") {
                workingDir = workingDir.parentFile
            }
            if (workingDir != null) {
                return workingDir
            }
        }
        val baseDir = myProject.baseDir
        return if (baseDir != null) VfsUtilCore.virtualToIoFile(baseDir) else null
    }

    override fun fetchLatestVersion(pkg: InstalledPackage, consumer: CatchingConsumer<String, Exception>) {
        myManager.fetchPackageInfo(
            object : NodePackageInfoManager.PackageInfoConsumer(pkg.name, false) {
                override fun onPackageInfo(packageInfo: NodePackageInfo?) {
                    if (packageInfo != null) {
                        consumer.consume(packageInfo.latestVersion)
                    } else {
                        consumer.consume(null as String?)
                    }
                }

                override fun onException(e: java.lang.Exception) {
                    consumer.consume(e)
                }
            }
        )
    }
}


fun main() {
    val output = """
        > nx-example@0.0.0 nx /Users/iguissouma/idea/nx-examples
        > nx "list"


         >  NX   Installed plugins:

           @angular-devkit/build-angular (executors)
           @ngrx/component-store (generators)
           @ngrx/effects (generators)
           @ngrx/entity (generators)
           @ngrx/router-store (generators)
           @ngrx/store (generators)
           @ngrx/store-devtools (generators)
           @nrwl/angular (executors,generators)
           @nrwl/cypress (executors,generators)
           @nrwl/jest (executors,generators)
           @nrwl/js (executors,generators)
           @nrwl/linter (executors,generators)
           @nrwl/nx-cloud (generators)
           @nrwl/react (executors,generators)
           @nrwl/storybook (executors,generators)
           @nrwl/web (executors,generators)
           @nrwl/workspace (executors,generators)
           nx (executors)


         >  NX   Also available:

           @nrwl/express (executors,generators)
           @nrwl/nest (executors,generators)
           @nrwl/next (executors,generators)
           @nrwl/node (executors,generators)
           @nrwl/nx-plugin (executors,generators)


         >  NX   Community plugins:

           nx-plugins - Nx plugin integrations with ESBuild / Vite / Snowpack / Prisma, with derived ESBuild / Snowpack / ... plugins.
           @codebrew/nx-aws-cdk - An Nx plugin for aws cdk develop.
           @rxap/plugin-localazy - An Nx plugin for localazy.com upload and download tasks.
           nx-electron - An Nx plugin for developing Electron applications
           nx-stylelint - Nx plugin to use stylelint in a nx workspace
           @nxtend/ionic-react - An Nx plugin for developing Ionic React applications and libraries
           @nxtend/ionic-angular - An Nx plugin for developing Ionic Angular applications and libraries
           @nxtend/capacitor - An Nx plugin for developing cross-platform applications using Capacitor
           @nxtend/firebase - An Nx plugin for developing applications using Firebase
           @angular-architects/ddd - Nx plugin for structuring a monorepo with domains and layers
           @offeringsolutions/nx-karma-to-jest - Nx plugin for replacing karma with jest in an Nx workspace
           @flowaccount/nx-serverless - Nx plugin for node/angular-universal schematics and deployment builders in an Nx workspace
           @ns3/nx-serverless - Nx plugin for node serverless applications in an Nx workspace
           @ns3/nx-jest-playwright - Nx plugin to run jest-playwright e2e tests in an Nx workspace
           @dev-thought/nx-deploy-it - Nx plugin to deploy applications on your favorite cloud provider
           @offeringsolutions/nx-protractor-to-cypress - Nx plugin to replace protractor with cypress in an nx workspace
           @nx-tools/nx-docker - Nx plugin to build docker images of your affected apps
           @angular-custom-builders/lite-serve - Nx plugin to run the e2e test on an existing dist folder
           @nx-plus/nuxt - Nx plugin adding first class support for Nuxt in your Nx workspace.
           @nx-plus/vue - Nx plugin adding first class support for Vue in your Nx workspace.
           @nx-plus/docusaurus - Nx plugin adding first class support for Docusaurus in your Nx workspace.
           @twittwer/compodoc - Nx Plugin to integrate the generation of documentation with Compodoc in the Nx workflow
           @nxext/svelte - Nx plugin to use Svelte within nx workspaces
           @nxext/stencil - Nx plugin to use StencilJs within nx workspaces
           @nxext/vite - Nx plugin to use ViteJS within nx workspaces
           @nxext/solid - Nx plugin to use SolidJS within nx workspaces
           @joelcode/gcp-function - Nx plugin to generate, test, lint, build, serve, & deploy Google Cloud Function
           @nx-go/nx-go - Nx plugin to use Go in a Nx workspace
           @angular-architects/module-federation - Nx plugin to use webpack module federation
           @nxrocks/nx-spring-boot - Nx plugin to generate, run, package, build (and more) Spring Boot projects inside your Nx workspace
           @trumbitta/nx-plugin-openapi - OpenAPI Plugin for Nx. Keep your API spec files in libs, and auto-generate sources.
           @trumbitta/nx-plugin-unused-deps - Check the dependency graph of your monorepo, looking for unused NPM packages.
           @nxrocks/nx-flutter - Nx Plugin adding first class support for Flutter in your Nx workspace
           @srleecode/domain - Nx Plugin for allowing operations to occur at the domain level instead of the default library level

           @jscutlery/semver - Nx plugin to automate semantic versioning and CHANGELOG generation.
           ngx-deploy-npm - Publish your libraries to NPM with just one command.
           @trafilea/nx-shopify - Nx plugin for developing performance-first Shopify themes
           nx-dotnet - Nx plugin for developing and housing .NET projects within an Nx workspace.
           @nxrocks/nx-quarkus - Nx plugin to generate, run, package, build (and more) Quarkus projects inside your Nx workspace
           @nx-extend/gcp-secrets - Nx plugin to generate and securely deploy your Google Cloud Secrets
           @nx-extend/gcp-storage - Nx plugin to upload to Google Cloud Storage
           @nx-extend/gcp-functions - Nx plugin to generate, run, build and deploy your Google Cloud Functions
           @nx-extend/gcp-deployment-manager - Nx plugin to deploy your Google Cloud Deployments
           @nx-extend/gcp-cloud-run - Nx plugin to build and deploy your docker container to Google Cloud Run
           @nx-extend/translations - Nx plugin to extract, pull, push and translate your apps translations
           @nativescript/nx - Nx Plugin adding first class support for NativeScript in your Nx workspace
           @nx-clean/plugin-core - Nx Plugin to generate projects following Clean Architecture practices
           @jnxplus/nx-boot-gradle - Nx plugin to add Spring Boot and Gradle multi-project builds support to Nx workspace
           @jnxplus/nx-boot-maven - Nx plugin to add Spring Boot and Maven multi-module project support to Nx workspace
           @nxtensions/astro - Nx plugin adding first class support for Astro (https://astro.build).
           @nxrs/cargo - Nx plugin adding first-class support for Rust applications and libraries.
           nx-uvu - An nx executor for the uvu test library


         >  NX   Use "nx list [plugin]" to find out more

        (node:94674) [DEP0148] DeprecationWarning: Use of deprecated folder mapping "./" in the "exports" field module resolution of the package at /Users/iguissouma/idea/nx-examples/node_modules/tslib/package.json.
        Update this package.json to use a subpath pattern like "./*".
        (Use `node --trace-deprecation ...` to show where the warning was created)
    """.trimIndent()

    val installed = getInstalled(output)
}
