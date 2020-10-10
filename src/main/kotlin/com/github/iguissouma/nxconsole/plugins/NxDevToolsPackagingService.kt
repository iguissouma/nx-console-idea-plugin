package com.github.iguissouma.nxconsole.plugins

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
import com.intellij.lang.javascript.JavaScriptBundle
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
import java.util.*

const val command = """

>  NX  Installed plugins:

  @angular-devkit/build-angular (builders)
  @nrwl/angular (builders,schematics)
  @nrwl/cypress (builders,schematics)
  @nrwl/jest (builders,schematics)
  @nrwl/linter (builders)
  @nrwl/workspace (builders,schematics)


>  NX  Also available:

  @nrwl/bazel (schematics)
  @nrwl/express (builders,schematics)
  @nrwl/nest (builders,schematics)
  @nrwl/next (builders,schematics)
  @nrwl/node (builders,schematics)
  @nrwl/nx-plugin (builders,schematics)
  @nrwl/react (builders,schematics)
  @nrwl/storybook (builders,schematics)
  @nrwl/web (builders,schematics)


>  NX  Community plugins:

  @nxtend/ionic-react - An Nx plugin for developing Ionic React applications and libraries
  @nxtend/capacitor - An Nx plugin for developing cross-platform applications using Capacitor
  @angular-architects/ddd - Nx plugin for structuring a monorepo with domains and layers
  @offeringsolutions/nx-karma-to-jest - Nx plugin for replacing karma with jest in an Nx workspace
  @flowaccount/nx-serverless - Nx plugin for node/angular-universal schematics and deployment builders in an Nx workspace
  @dev-thought/nx-deploy-it - Nx plugin to deploy applications on your favorite cloud provider
  @offeringsolutions/nx-protractor-to-cypress - Nx plugin to replace protractor with cypress in an nx workspace
  @nx-tools/nx-docker - Nx plugin to build docker images of your affected apps
  @angular-custom-builders/lite-serve - Nx plugin to run the e2e test on an existing dist folder
  @nx-plus/nuxt - Nx plugin adding first class support for Nuxt in your Nx workspace.
  @nx-plus/vue - Nx plugin adding first class support for Vue in your Nx workspace.
  @nx-plus/docusaurus - Nx plugin adding first class support for Docusaurus in your Nx workspace.
  @twittwer/compodoc - Nx Plugin to integrate the generation of documentation with Compodoc in the Nx workflow
  @nxext/stencil - Nx plugin to use StencilJs within nx workspaces
  @joelcode/gcp-function - Nx plugin to generate, test, lint, build, serve, & deploy Google Cloud Function
  @nx-go/nx-go - Nx plugin to use Go in a Nx workspace


>  NX   NOTE  Use "nx list [plugin]" to find out more

    
"""

fun getPackageList(nxListOutput: String, from: String, to: String, trim: String): List<String> {
    val i = nxListOutput.indexOf(from) + from.length
    return nxListOutput.substring(i, nxListOutput.indexOf(to))
        .split("\n")
        .map { it.trim() }
        .filterNot { it.isEmpty() }
        .map { it.substring(0, it.indexOf(trim)).trim() }
        .toList()
}

fun main() {
    val message = getInstalled()
    println(message)

}

private fun getInstalled() = getPackageList(command, ">  NX  Installed plugins:", ">  NX  Also available:", " (")
private fun getAlsoAvailable() = getPackageList(command, ">  NX  Also available:", ">  NX  Community plugins:", " (")
private fun getCommunityPlugins() = getPackageList(
    command,
    ">  NX  Community plugins:",
    ">  NX   NOTE  Use \"nx list [plugin]\" to find out more",
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
        return (getInstalled()+ getAlsoAvailable()+ getCommunityPlugins())
            .map { RepoPackage(it, "", "") }
            .toMutableList()
    }

    override fun reloadAllPackages(): MutableList<RepoPackage> {
        return this.allPackages

    }

    override fun getInstalledPackages(): MutableCollection<InstalledPackage> {
        val installed = getInstalled()
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
                    myProject, myInterpreter, repoPackage!!.name, version, VfsUtilCore.virtualToIoFile(baseDir),
                    listener!!, extraOptions
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
                myProject, myInterpreter, repoPackage.name, version, workingDir,
                listener!!, extraOptions
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
        this.myManager.fetchPackageInfo(object : NodePackageInfoManager.PackageInfoConsumer(packageName) {
            override fun onPackageInfo(packageInfo: NodePackageInfo?) {
                if (packageInfo != null) {
                    consumer!!.consume(packageInfo.versions)
                }
            }

            override fun onException(e: java.lang.Exception) {
                consumer!!.consume(e)
            }
        })
    }

    override fun fetchPackageDetails(packageName: String?, consumer: CatchingConsumer<String, Exception>?) {
        myManager.fetchPackageInfo(object : NodePackageInfoManager.PackageInfoConsumer(packageName!!) {
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
        })
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
            val message = JavaScriptBundle.message(
                "node.packages.cannot_find_working_directory.text",
                packageName, args
            )
           LOG.warn(message)
            ApplicationManager.getApplication().invokeLater({
                listener.operationFinished(
                    packageName,
                    ErrorDescription.fromMessage(message)
                )
            }, ModalityState.any())
        } else {
            val commandLine = PackageInstaller.computeAndReportIfFailed<GeneralCommandLine, ExecutionException>(
                listener, packageName
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
                    ApplicationManager.getApplication().invokeLater({
                        listener.operationFinished(
                            packageName,
                            ErrorDescription.fromMessage(errorMessageRef.get() as String)
                        )
                        LocalFileSystem.getInstance().refresh(true)
                    }, ModalityState.any())
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
        myManager.fetchPackageInfo(object : NodePackageInfoManager.PackageInfoConsumer(pkg.name, false) {
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
        })
    }


}
