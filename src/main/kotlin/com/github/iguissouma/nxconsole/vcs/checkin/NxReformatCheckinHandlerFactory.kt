package com.github.iguissouma.nxconsole.vcs.checkin

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.vcs.NxVcsConfiguration
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.ui.BooleanCommitOption
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.vcs.checkin.CheckinHandlerUtil
import com.intellij.openapi.vcs.checkin.CheckinMetaHandler
import com.intellij.openapi.vcs.ui.RefreshableOnComponent

class NxReformatCheckinHandlerFactory : CheckinHandlerFactory() {
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler =
        NxReformatBeforeCheckinHandler(panel.project, panel)
}

class NxReformatBeforeCheckinHandler(val myProject: Project, val panel: CheckinProjectPanel) :
    CheckinHandler(),
    CheckinMetaHandler {

    private val settings get() = NxVcsConfiguration.getInstance(project = myProject)

    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent =
        BooleanCommitOption(
            panel,
            NxBundle.message("nx.checkbox.checkin.options.reformat.code"),
            true,
            settings::NX_REFORMAT_BEFORE_PROJECT_COMMIT
        )

    override fun runCheckinHandlers(runnable: Runnable) {
        val saveAndContinue = {
            FileDocumentManager.getInstance().saveAllDocuments()
            runnable.run()
        }

        if (settings.NX_REFORMAT_BEFORE_PROJECT_COMMIT && !DumbService.isDumb(myProject)) {
            NxReformatCodeProcessor(
                myProject,
                CheckinHandlerUtil.getPsiFiles(myProject, panel.virtualFiles),
                saveAndContinue
            ).run()
        } else {
            saveAndContinue() // TODO just runnable.run()?
        }
    }
}
