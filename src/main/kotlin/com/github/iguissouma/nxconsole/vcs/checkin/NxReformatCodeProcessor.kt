package com.github.iguissouma.nxconsole.vcs.checkin

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.util.NxExecutionUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.ProgressText
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.vcsUtil.VcsFileUtil

class NxReformatCodeProcessor(val myProject: Project, val psiFiles: Array<PsiFile>, val myPostRunnable: Runnable) {

    fun run() {

        FileDocumentManager.getInstance().saveAllDocuments()
        val isSuccess = ProgressManager.getInstance().runProcessWithProgressSynchronously(
            ThrowableComputable<Boolean, RuntimeException> {
                val indicator = ProgressManager.getInstance().progressIndicator
                val projectBasePath: VirtualFile =
                    getBasePathAsVirtualFile(myProject) ?: return@ThrowableComputable false
                val files = psiFiles.map { it.virtualFile }
                    .flatMap { f: VirtualFile? -> VfsUtil.collectChildrenRecursively(f!!) }
                    .mapNotNull { f: VirtualFile? -> VcsFileUtil.getRelativeFilePath(f, projectBasePath) }
                    .toList()
                    .joinToString(",")
                NxExecutionUtil(myProject).execute("format:write", "--files=$files")
            },
            getProgressTitle(),
            true,
            myProject
        )
        if (isSuccess) {
            myPostRunnable.run()
        }
    }

    private fun getProgressTitle(): @ProgressText String {
        return NxBundle.message("nx.reformat.progress.common.text")
    }

    private fun getBasePathAsVirtualFile(project: Project): VirtualFile? {
        val basePath = project.basePath
        return if (basePath == null) null else LocalFileSystem.getInstance().findFileByPath(basePath)
    }
}
