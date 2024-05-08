package com.koxudaxi.ruff

import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider
import java.io.File


class RuffLanguageServer(project: Project) : ProcessStreamConnectionProvider() {
    init {
        val ruffConfigService = RuffConfigService.getInstance(project)
        val commands = createCommand(project, ruffConfigService)
        if (commands != null) {
            if (ruffConfigService.useRuffFormat) {
                super.setUserEnvironmentVariables(mapOf("RUFF_EXPERIMENTAL_FORMATTER" to "1"))
            }
            super.setCommands(commands)
        }


    }

    private fun createCommand(project: Project, ruffConfigService: RuffConfigService): List<String>? {
        if (!ruffConfigService.useRuffLsp) return null
        if (!isInspectionEnabled(project)) return null
        val executable =
                ruffConfigService.ruffLspExecutablePath?.let { File(it) }?.takeIf { it.exists() }
                        ?: detectRuffExecutable(
                                project, ruffConfigService, true
                        ) ?: return null
        // TODO Support Configuration
        return listOf(executable.absolutePath)
    }

    // TODO: move the function to new ruff lsp class
    private fun isInspectionEnabled(project: Project): Boolean {
        val inspectionProfileManager = ProjectInspectionProfileManager.getInstance(project)

        val toolWrapper = InspectionToolRegistrar.getInstance().createTools()
                .find { it.shortName == RuffInspection.INSPECTION_SHORT_NAME } ?: return false
        return inspectionProfileManager.currentProfile.isToolEnabled(toolWrapper.displayKey)
    }
}