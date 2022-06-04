package com.github.iguissouma.nxconsole.execution

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.cli.config.NxProject
import com.github.iguissouma.nxconsole.cli.config.WorkspaceType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.diff.util.FileEditorBase
import com.intellij.ide.FileIconProvider
import com.intellij.ide.actions.SplitAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.TextFieldWithAutoCompletionListProvider
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.util.ThrowableRunnable
import java.awt.BorderLayout
import java.lang.reflect.InvocationTargetException
import java.util.*
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities


class NxUiIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        if (project == null) {
            return null
        }
        return (file as? NxUiFile)?.let {
            return NxConfigProvider.getNxWorkspaceType(project, file)
                .let { if (it == WorkspaceType.ANGULAR) angular else nrwl }
        }

    }

    companion object {
        val angular = NxIcons.ANGULAR
        val nrwl = NxIcons.NRWL_ICON
    }

}

class NxUiFileType : FileType {
    override fun getName(): String = "NxUi"
    override fun getDescription(): String = ""
    override fun getDefaultExtension(): String = ".nx"
    override fun getIcon(): Icon = NxIcons.NRWL_ICON
    override fun isBinary(): Boolean = true
    override fun isReadOnly(): Boolean = true
    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    companion object {
        val INSTANCE = NxUiFileType()
    }
}

abstract class NxUiFile(name: String) : LightVirtualFile(name, NxUiFileType.INSTANCE, "") {
    init {
        isWritable = false
    }

    abstract fun createMainComponent(project: Project): JComponent
}

internal class DefaultNxUiFile(val task: String, panel: NxUiPanel) : NxUiFile(task) {
    private var nxUiPanel: NxUiPanel? = null

    init {
        nxUiPanel = panel
        // Disposer.register(panel.getUi(), Disposable { nxUiPanel = null })

        putUserData(SplitAction.FORBID_TAB_SPLIT, true)
    }

    override fun createMainComponent(project: Project): JComponent {
        return nxUiPanel ?: JBPanelWithEmptyText().withEmptyText(NxBundle.message("nx.ui.tab.closed.status"))
    }

    override fun isValid(): Boolean = nxUiPanel != null
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DefaultNxUiFile

        if (task != other.task) return false

        return true
    }

    override fun hashCode(): Int {
        return task.hashCode()
    }
}

open class NxUiPanel(val project: Project, args: MutableList<String>) :
    JPanel(BorderLayout()),
    Disposable {

    var centerPanel: DialogPanel? = null
    override fun dispose() {
    }
}

class NxUIEditor(private val project: Project, private val nxUiFile: NxUiFile) : FileEditorBase() {
    private val createMainComponent: JComponent = nxUiFile.createMainComponent(project)
    private val rootComponent: JComponent = JPanel(BorderLayout()).also {
        it.add(createMainComponent, BorderLayout.CENTER)
    }

    override fun getComponent(): JComponent = rootComponent
    override fun getPreferredFocusedComponent(): JComponent? =
        (createMainComponent as? NxUiPanel)?.centerPanel?.preferredFocusedComponent

    override fun getName(): String = NxBundle.message("nx.ui.editor.name")
    override fun getFile() = nxUiFile
}

class NxUIEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file is NxUiFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return NxUIEditor(project, file as NxUiFile)
    }

    override fun getEditorTypeId(): String = "NxUIEditor"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

// a stupid options parser
/**
 * "--directory x --dryRun --dire=y" ->
 */
fun parseOptions(args: Array<String>): MutableMap<String, List<String>> {
    val params = mutableMapOf<String, List<String>>()
    var options: MutableList<String>? = null
    for (i in 0 until args.size) {
        val a = args[i]
        if (a[0] == '-') {
            if (a.length < 2) {
                continue
            }
            options = mutableListOf()
            if (a.contains("=")) {
                val endIndex = a.indexOf("=")
                options.add(a.substring(endIndex + 1))
                if (a[1] == '-') {
                    params[a.substring(2, endIndex)] = options
                } else {
                    params[a.substring(1, endIndex)] = options
                }
            } else {
                if (a[1] == '-') {
                    params[a.substring(2)] = options
                } else {
                    params[a.substring(1)] = options
                }
            }
        } else if (options != null) {
            options.add(a)
        }
    }
    return params
}

// a stupid arg parser
/**
 * "--directory x --dryRun --dire=y" ->
 */
fun parseArguments(args: Array<String>): MutableList<String> {
    val options: MutableList<String> = mutableListOf()
    for (element in args) {
        if (element[0] == '-') {
            break
        } else {
            options.add(element)
        }
    }
    return options
}

class SchematicProjectOptionsTextField(
    project: Project?,
    options: List<NxProject>
) : TextFieldWithAutoCompletion<NxProject>(project, SchematicProjectCompletionProvider(options), false, null)

private class SchematicProjectCompletionProvider(options: List<NxProject>) :
    TextFieldWithAutoCompletionListProvider<NxProject>(
        options
    ) {

    override fun getLookupString(item: NxProject): String {
        return item.name
    }

    override fun getTypeText(item: NxProject): String? {
        return item.type?.name?.lowercase(Locale.getDefault())
    }

    override fun compare(item1: NxProject, item2: NxProject): Int {
        return StringUtil.compare(item1.name, item2.name, false)
    }

    override fun createLookupBuilder(item: NxProject): LookupElementBuilder {
        return super.createLookupBuilder(item)
            .withTypeIconRightAligned(true)
    }

    override fun getIcon(item: NxProject): Icon {
        return if (item.type == NxProject.AngularProjectType.APPLICATION) NxIcons.NX_APP_FOLDER else NxIcons.NX_LIB_FOLDER
    }
}

class EdtTestUtil {
    companion object {
        @JvmStatic
        fun <V> runInEdtAndGet(computable: ThrowableComputable<V, Throwable>): V =
            runInEdtAndGet { computable.compute() }

        @JvmStatic
        fun runInEdtAndWait(runnable: ThrowableRunnable<Throwable>) {
            runInEdtAndWait { runnable.run() }
        }
    }
}

/**
 * Consider using Kotlin coroutines and `com.intellij.openapi.application.AppUIExecutor.onUiThread().coroutineDispatchingContext()`
 * @see com.intellij.openapi.application.AppUIExecutor.onUiThread
 */
fun <V> runInEdtAndGet(compute: () -> V): V {
    var v: V? = null
    runInEdtAndWait { v = compute() }
    return v!!
}

/**
 * Consider using Kotlin coroutines and `com.intellij.openapi.application.AppUIExecutor.onUiThread().coroutineDispatchingContext()`
 * @see com.intellij.openapi.application.AppUIExecutor.onUiThread
 */
fun runInEdtAndWait(runnable: () -> Unit) {
    val app = ApplicationManager.getApplication()
    if (app is Application) {
        if (app.isDispatchThread) {
            // reduce stack trace
            runnable()
        } else {
            var exception: Throwable? = null
            app.invokeAndWait {
                try {
                    runnable()
                } catch (e: Throwable) {
                    exception = e
                }
            }

            exception?.let { throw it }
        }
        return
    }

    if (SwingUtilities.isEventDispatchThread()) {
        runnable()
    } else {
        try {
            SwingUtilities.invokeAndWait { runnable() }
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }
}
