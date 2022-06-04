package com.github.iguissouma.nxconsole.depGraph

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.cli.config.NxProject
import com.github.iguissouma.nxconsole.util.replacePnpmToPnpx
import com.intellij.execution.ExecutionException
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ScriptRunnerUtil
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo
import com.intellij.ide.util.ElementsChooser
import com.intellij.javascript.debugger.CommandLineDebugConfigurator
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.npm.NpmNodePackage
import com.intellij.javascript.nodejs.npm.NpmPackageDescriptor
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.npm.WorkingDirectoryDependentNpmPackageVersionManager
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.javascript.JavaScriptBundle
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.AppUIUtil
import com.intellij.ui.InplaceButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefClient
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.ui.popup.util.PopupState
import com.intellij.util.ThreeState
import com.intellij.util.TimeoutUtil
import com.intellij.util.text.SemVer
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.List
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class NxDepGraphToolWindow : ToolWindowFactory {

    override fun isApplicable(project: Project): Boolean {
        return super.isApplicable(project) && NxJsonUtil.findChildNxJsonFile(project.baseDir) != null
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return super.shouldBeAvailable(project) && NxJsonUtil.findChildNxJsonFile(project.baseDir) != null
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val nxDepGraphWindowService = ServiceManager.getService(project, NxDepGraphWindowService::class.java)
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content: Content =
            contentFactory.createContent(nxDepGraphWindowService.nxDepGraphWindow.content(), "", false)

        /*val scrollFromEditorAction: AnAction = object : DumbAwareAction(
            NxBundle.messagePointer(
                "nx.depgraph.focus.from.editor.text",
            ),
            NxBundle.messagePointer(
                "nx.depgraph.focus.project.open.in.active.editor.description",
            ),
            AllIcons.General.Locate
        ) {
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabledAndVisible = true
            }

            override fun actionPerformed(e: AnActionEvent) {
            }
        }
        (toolWindow as ToolWindowEx).setTitleActions(listOf(scrollFromEditorAction))
        */

        toolWindow.contentManager.addContent(content)
    }
}

class NxDepGraphWindowService(val project: Project) {
    val nxDepGraphWindow = NxDepGraphWindow(project)
}

class NxDepGraphWindow(val project: Project) {

    var depGraphProcess: KillableProcessHandler? = null

    val isProcessRunning: Boolean
        get() = depGraphProcess?.isProcessTerminated?.not() ?: false

    fun content(): JComponent {
        val actionGroup = DefaultActionGroup()

        val browser = JBCefBrowser()
        val browserComponent = browser.component
        // https://youtrack.jetbrains.com/issue/JBR-3175
        browser.jbCefClient.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, 10)
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(
                ProjectManager.TOPIC,
                object : ProjectManagerListener {
                    override fun projectClosed(project: Project) {
                        if (isProcessRunning) {
                            stopDepGraphServer()
                        }
                    }
                }
            )
        val stop: AnAction = object : AnAction("Stop Dep Graph", "", AllIcons.Actions.Suspend) {

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = isProcessRunning
            }

            override fun actionPerformed(e: AnActionEvent) {
                stopDepGraphServer()
            }
        }

        val run: AnAction = object : AnAction("Run Dep Graph", "", AllIcons.Actions.Execute) {

            override fun update(e: AnActionEvent) {
                val runningServer = isProcessRunning
                val presentation = e.presentation
                if (runningServer) {
                    // TODO check if we add re-run
                    // presentation.icon = AllIcons.Actions.Restart
                    // presentation.text = "Rerun Dep Graph"
                    e.presentation.isEnabled = false
                } else {
                    presentation.icon = AllIcons.Actions.Execute
                    presentation.text = "Run Dep Graph"
                    e.presentation.isEnabled = true
                }
            }

            private fun configureCommandLine(targetRun: NodeTargetRun, npmPkg: NodePackage, workingDirectory: File) {
                targetRun.enableWrappingWithYarnPnpNode = false
                val commandLine = targetRun.commandLineBuilder
                commandLine.setCharset(StandardCharsets.UTF_8)
                commandLine.setWorkingDirectory(targetRun.path(workingDirectory.absolutePath))
                NpmNodePackage.configureNpmPackage(targetRun, npmPkg, *arrayOfNulls(0))
                val yarn = NpmUtil.isYarnAlikePackage(npmPkg)
                if (NpmUtil.isPnpmPackage(npmPkg)) {
                    var version: SemVer? = null
                    WorkingDirectoryDependentNpmPackageVersionManager.getInstance(project)
                        .fetchVersion(targetRun.interpreter, npmPkg, workingDirectory) {
                            version = it
                        }

                    // version is null first time use exec
                    if (version == null || (version!!.major >= 6 && version!!.minor >= 13)) {
                        // useExec like vscode extension
                        commandLine.addParameter("exec")
                    } else {
                        // use pnpx
                        NpmNodePackage(replacePnpmToPnpx(npmPkg.systemIndependentPath))
                            .let {
                                if (it.isValid(targetRun.project, targetRun.interpreter)) {
                                    it.configureNpmPackage(targetRun)
                                }
                            }
                    }
                } else if (yarn.not()) {
                    NpmPackageDescriptor.findBinaryFilePackage(targetRun.interpreter, "npx")
                        ?.configureNpmPackage(targetRun)
                }
                commandLine.addParameter("nx")
            }

            override fun actionPerformed(e: AnActionEvent) {
                val toolWindowManager = ToolWindowManager.getInstance(project)
                val toolWindow = toolWindowManager.getToolWindow("Nx Dep Graph")
                val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return
                val targetRun = NodeTargetRun(
                    interpreter, project, null as CommandLineDebugConfigurator?,
                    NodeTargetRun.createOptions(
                        ThreeState.NO, List.of()
                    )
                )

                val npmPackageRef = NpmUtil.createProjectPackageManagerPackageRef()
                val npmPkg = NpmUtil.resolveRef(npmPackageRef, targetRun.project, targetRun.interpreter)
                if (npmPkg == null) {
                    if (NpmUtil.isProjectPackageManagerPackageRef(npmPackageRef)) {
                        val message = JavaScriptBundle.message(
                            "npm.dialog.message.cannot.resolve.package.manager",
                            NpmManager.getInstance(project).packageRef.identifier
                        )
                        throw NpmManager.InvalidNpmPackageException(
                            project,
                            HtmlBuilder().append(message).append(HtmlChunk.p())
                                .toString() + JavaScriptBundle.message(
                                "please.specify.package.manager",
                                *arrayOfNulls(0)
                            )
                        ) {} // onNpmPackageRefResolved
                    } else {
                        throw ExecutionException(
                            JavaScriptBundle.message(
                                "npm.dialog.message.cannot.resolve.package.manager",
                                npmPackageRef.identifier
                            )
                        )
                    }
                }

                configureCommandLine(targetRun, npmPkg, File(project.basePath!!))
                val commandLine = targetRun.commandLineBuilder
                commandLine.addParameters("dep-graph", "--port=4222", "--open=false", "--watch")
                val processWithCmdLine = targetRun.startProcessEx()

                ApplicationManager.getApplication().invokeLater {
                    runInEdt {
                        depGraphProcess = processWithCmdLine.processHandler
                        depGraphProcess!!.setShouldDestroyProcessRecursively(true)
                        depGraphProcess!!.addProcessListener(object : ProcessListener {
                            override fun startNotified(event: ProcessEvent) {
                                UIUtil.invokeLaterIfNeeded {
                                    toolWindow!!.setIcon(ExecutionUtil.getLiveIndicator(NxIcons.NRWL_ICON))
                                }
                            }

                            override fun processTerminated(event: ProcessEvent) {
                                AppUIUtil.invokeLaterIfProjectAlive(project) {
                                    toolWindow?.setIcon(NxIcons.NRWL_ICON)
                                }
                                loadServerNotStartedPage(browser)
                            }

                            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                                if (event.text.contains("Dep graph started") || event.text.contains("Project graph started")) {
                                    TimeoutUtil.sleep(500)
                                    browser.loadURL("http://localhost:4222/")
                                    // depGraphProcess?.removeProcessListener(this)
                                }
                            }
                        })
                        depGraphProcess?.startNotify()
                    }
                }
            }
        }

        val filter = object : DumbAwareAction("Filter", "", AllIcons.General.Filter) {
            private val myPopupState = PopupState()

            fun showPopup(popup: JBPopup, editor: Editor?, event: AnActionEvent?) {
                val inputEvent = event?.inputEvent
                val eventSource = inputEvent?.source
                if (editor != null && editor.component.isShowing) {
                    popup.showInBestPositionFor(editor)
                } else if (event != null) {
                    if (eventSource !is InplaceButton && eventSource !is ActionButton) {
                        popup.showInBestPositionFor(event.dataContext)
                    } else {
                        popup.setMinimumSize((eventSource as JComponent).size)
                        popup.showUnderneathOf((eventSource as Component?)!!)
                    }
                }
            }

            override fun actionPerformed(e: AnActionEvent) {
                if (!this.myPopupState.isRecentlyHidden) {

                    val chooser = createKindChooser(e)
                    val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(chooser, null as JComponent?)
                        .setFocusable(false).setRequestFocus(false).setResizable(true).setMinSize(
                            Dimension(200, 200)
                        ).setDimensionServiceKey(project, "NxDepGraphViewActions_Filter", false).addListener(
                            myPopupState
                        ).createPopup()

                    showPopup(popup, e.getData(CommonDataKeys.EDITOR), e)
                }
            }

            private fun createKindChooser(e: AnActionEvent): JComponent {
                val p = e.project ?: return JPanel()
                val projects = NxConfigProvider.getNxConfig(p, p.baseDir)?.projects ?: emptyList()

                val chooser: ElementsChooser<NxProject> = object : ElementsChooser<NxProject>(projects, false) {
                    override fun getItemText(value: NxProject): String {
                        return StringUtil.capitalizeWords(value.name, true)
                    }

                    override fun getItemIcon(value: NxProject): @Nullable Icon {
                        return if (value.type == NxProject.AngularProjectType.APPLICATION) NxIcons.NX_APP_FOLDER
                        else NxIcons.NX_LIB_FOLDER
                    }
                }

                chooser.addElementsMarkListener(
                    ElementsChooser.ElementsMarkListener { element, isMarked ->
                        val cefBrowser = browser.cefBrowser
                        cefBrowser.executeJavaScript(
                            "document.querySelector('input[value=${element?.name}]').checked = $isMarked;",
                            cefBrowser.url,
                            0
                        )
                    }
                )

                // Create a JS query instance
                val jsQueryGetSelectedProjects = JBCefJSQuery.create(browser)
                // Add a query handler
                jsQueryGetSelectedProjects.addHandler { selectedProjects: String? ->
                    val selectedProjectNames = selectedProjects?.split(";") ?: emptyList()
                    NxConfigProvider.getNxConfig(project, project.baseDir)?.projects?.forEach {
                        chooser.setElementMarked(it, it.name in selectedProjectNames)
                    }
                    null // can respond back to JS with JBCefJSQuery.Response
                }
                val cefBrowser = browser.cefBrowser
                // Inject the query callback into JS
                cefBrowser.executeJavaScript(
                    """
                          var elements = document.querySelectorAll('input[name=projectName]:checked');
                          var selectedProjects = Array.from(elements).map(element => element.value).join(';');
                          ${jsQueryGetSelectedProjects.inject("selectedProjects")}
                    """.trimIndent(),
                    cefBrowser.url, 0
                )

                // Dispose the query when necessary
                Disposer.dispose(jsQueryGetSelectedProjects)

                chooser.isFocusable = false

                val panel = JPanel()
                panel.layout = BoxLayout(panel, 1)
                panel.add(chooser)
                val buttons = JPanel()
                val all = JButton(NxBundle.message("projects.filter.all"))
                all.addActionListener { chooser.setAllElementsMarked(true) }
                buttons.add(all)
                val none = JButton(NxBundle.message("projects.filter.none"))
                none.addActionListener { chooser.setAllElementsMarked(false) }
                buttons.add(none)
                val invert = JButton(NxBundle.message("projects.filter.invert"))
                invert.addActionListener { chooser.invertSelection() }
                buttons.add(invert)
                panel.add(buttons)

                return panel
            }
        }

        val refresh = object : DumbAwareAction("Refresh", "", AllIcons.Actions.Refresh) {

            override fun actionPerformed(e: AnActionEvent) {
                if (isProcessRunning) {
                    browser.cefBrowser.reload()
                }
            }
        }

        val messageBus = project.messageBus
        messageBus.connect()
            .subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                object : FileEditorManagerListener {
                    override fun fileOpened(@NotNull source: FileEditorManager, @NotNull file: VirtualFile) {
                        if (isProcessRunning) {
                            focusProject(file)
                        }
                    }

                    private fun focusProject(file: VirtualFile) {
                        val nxProject = NxConfigProvider.getNxProject(project, file) ?: return
                        val cefBrowser = browser.cefBrowser
                        // cefBrowser.executeJavaScript("window.focusedProject = '${nxProject.name}'", cefBrowser.url, 0)
                        cefBrowser.executeJavaScript("focusProject('${nxProject.name}')", cefBrowser.url, 0)
                    }

                    override fun fileClosed(@NotNull source: FileEditorManager, @NotNull file: VirtualFile) {
                    }

                    override fun selectionChanged(@NotNull event: FileEditorManagerEvent) {
                        if (isProcessRunning) {
                            event.newFile?.also { focusProject(it) }
                        }
                    }
                }
            )

        actionGroup.add(run)
        actionGroup.add(stop)
        actionGroup.addSeparator()
        actionGroup.add(refresh)
        actionGroup.addSeparator()
        // TODO add filter when synchro between web and swing is always ok
        // actionGroup.add(filter)
        val toolbar = ActionManager.getInstance().createActionToolbar("NxDepGraphPanel", actionGroup, true)
        toolbar.layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
        // Display a 'Server not started' message.
        val panel = JPanel(BorderLayout())
        toolbar.targetComponent = panel

        val label = JBLabel("Server not started", SwingConstants.CENTER)
        label.foreground = UIUtil.getLabelDisabledForeground()
        // panel.add(label, BorderLayout.CENTER)

        panel.add(toolbar.component, BorderLayout.NORTH)
        panel.add(browserComponent, BorderLayout.CENTER)
        loadServerNotStartedPage(browser)
        return panel
    }

    private fun loadServerNotStartedPage(browser: JBCefBrowser) {
        val color = if (LafManager.getInstance().currentLookAndFeel is DarculaLookAndFeelInfo)
            "#3C3F41" else "#F5F5F5"
        browser.loadHTML(
            """
                <html lang="en">
                    <head>
                        <style>
                            html { background-color: $color; } 
                            body {
                                min-height: 100vh;
                                max-width: 100bh;
                                background-color: $color; 
                                margin: 0 auto;
                            }
                        </style>
                    </head>
                    <body>
                        <p style='color:gray;padding-top:150px;text-align:center'>
                            Server not started.
                        </p>
                    </body>
                </html>
            """.trimIndent()
        )
    }

    private fun stopDepGraphServer() {
        if (depGraphProcess != null) {
            if (depGraphProcess!!.isProcessTerminated.not()) {
                ApplicationManager.getApplication().executeOnPooledThread {
                    if (depGraphProcess!!.isProcessTerminated.not()) {
                        ScriptRunnerUtil.terminateProcessHandler(
                            depGraphProcess!!,
                            1000,
                            depGraphProcess!!.commandLine
                        )
                        depGraphProcess = null
                    }
                }
            }
        }
    }
}
