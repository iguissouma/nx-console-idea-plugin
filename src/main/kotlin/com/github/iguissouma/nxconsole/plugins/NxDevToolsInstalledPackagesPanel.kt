package com.github.iguissouma.nxconsole.plugins

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.ObjectUtils
import com.intellij.webcore.packaging.InstalledPackagesPanel
import com.intellij.webcore.packaging.ManagePackagesDialog
import com.intellij.webcore.packaging.PackageManagementService
import com.intellij.webcore.packaging.PackagesNotificationPanel
import java.io.File

class NxDevToolsInstalledPackagesPanel(myProject: Project, myPackagesNotificationPanel: PackagesNotificationPanel) :
    InstalledPackagesPanel(myProject, myPackagesNotificationPanel) {

    init {
        myPackagesTable.setShowGrid(false)
    }

    override fun createManagePackagesDialog(): ManagePackagesDialog {
        val createManagePackagesDialog = super.createManagePackagesDialog()
        createManagePackagesDialog.setOptionsText("--save-dev --save-exact")
        return createManagePackagesDialog
    }

    override fun updatePackages(packageManagementService: PackageManagementService?) {
        val nxDevToolsService = ObjectUtils.tryCast(
            packageManagementService,
            NxDevToolsPackagingService::class.java
        )
        if (nxDevToolsService != null) {
            saveNxDevToolsConfig(nxDevToolsService)
        }
        myInstallButton.isEnabled = nxDevToolsService != null
        super.updatePackages(packageManagementService)
    }

    private fun saveNxDevToolsConfig(nxDevToolsService: NxDevToolsPackagingService) {
        ApplicationManager.getApplication().runWriteAction {
            val file: File = File(nxDevToolsService.getSettings().myNxJsonPath!!)
            if (file.isFile) {
                val vFile = VfsUtil.findFileByIoFile(file, false)
                if (vFile != null && vFile.isValid) {
                    val fileDocumentManager = FileDocumentManager.getInstance()
                    val document = fileDocumentManager.getDocument(vFile)
                    if (document != null) {
                        fileDocumentManager.saveDocument(document)
                    }
                }
            }
        }
    }
}
