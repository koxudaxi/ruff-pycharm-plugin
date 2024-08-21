package com.koxudaxi.ruff.lsp.lsp4ij

import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.*
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider


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
        if (!ruffConfigService.useRuffLsp && !ruffConfigService.useRuffServer) return null
        if (!ruffConfigService.useLsp4ij) return null
        if (!isInspectionEnabled(project)) return null

        val ruffCacheService = RuffCacheService.getInstance(project)
        if (ruffCacheService.getVersion() == null) return null
        val executable =
            when {
                ruffConfigService.useRuffServer && ruffCacheService.hasLsp() == true -> {
                    getRuffExecutable(project, ruffConfigService, false)
                }

                else -> getRuffExecutable(project, ruffConfigService, true)
            } ?: return null
        val executableCommandArgs = listOf(executable.absolutePath) + LSP_PREVIEW_ARGS

        return getConfigArgs(ruffConfigService)?.let {
            executableCommandArgs + it
        } ?: executableCommandArgs
    }
}