package com.github.iguissouma.nxconsole.buildTools

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.lang.javascript.buildTools.base.JsbtService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.ManagingFS
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import com.intellij.util.SmartList


class NxJsbtStartupActivity : StartupActivity.DumbAware {
    companion object{
        private val LOG = Logger.getInstance(
            NxJsbtStartupActivity::class.java
        )
        private fun setAvailable(project: Project, service: JsbtService, buildfilesToAdd: List<VirtualFile>) {
            ApplicationManager.getApplication().invokeLater({
                buildfilesToAdd.forEach {
                    service.fileManager.addBuildfile(it)
                }
                service.toolWindowManager.setAvailable()
            }, project.disposed)
        }

        private fun scheduleDetection(project: Project, servicesForDetection: List<JsbtService>) {
            ApplicationManager.getApplication().invokeLater({
                RefreshQueue.getInstance().refresh(
                    true, true,
                    {
                        ApplicationManager.getApplication().executeOnPooledThread {
                            doDetect(
                                project,
                                servicesForDetection
                            )
                        }
                    }, *ManagingFS.getInstance().getRoots(LocalFileSystem.getInstance())
                )
            }, ModalityState.NON_MODAL, project.disposed)
        }

        private fun doDetect(project: Project, servicesForDetection: List<JsbtService>) {

            ApplicationManager.getApplication().runReadAction {
                if (!project.isDisposed) {
                    servicesForDetection.forEach { service ->
                        // TODO check why shoul I put false as first argument
                        val buildfiles = service.detectAllBuildfilesInContentRoots(false, true)
                        LOG.info("Found " + buildfiles + " for " + service.javaClass.name)
                        if (!buildfiles.isEmpty() && NodeJsInterpreterManager.getInstance(project).isInterpreterAvailable) {
                            setAvailable(project, service, buildfiles)
                        }
                    }
                }
            }
        }
    }

    override fun runActivity(project: Project) {
        val servicesForDetection: MutableList<JsbtService> = SmartList<JsbtService>()

        val service = NxService.getInstance(project)
        val fileManager = service.fileManager
        if (fileManager.hasBuildfiles()) {
            setAvailable(project, service, emptyList())
        } else if (!fileManager.isDetectionDone) {
            fileManager.setDetectionDone()
            servicesForDetection.add(service)
            LOG.info("Detecting buildfiles for " + fileManager.javaClass.name)
        }

        if (!servicesForDetection.isEmpty()) {
            scheduleDetection(project, servicesForDetection)
        }

    }

}
