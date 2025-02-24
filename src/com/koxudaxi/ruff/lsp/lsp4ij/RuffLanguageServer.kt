package com.koxudaxi.ruff.lsp.lsp4ij

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.*
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider


class RuffLanguageServer(project: Project) : ProcessStreamConnectionProvider() {
    init {
        val ruffConfigService = project.configService
        val commands = createCommand(project, ruffConfigService)

        // Add null check here and log if null
        if (commands == null || commands.isEmpty()) {
            RuffLoggingService.log(
                project,
                "Failed to create LSP commands - check Ruff configuration",
                ConsoleViewContentType.ERROR_OUTPUT
            )
            // Set default value for commands even if null (for error display)
            super.setCommands(listOf("echo", "Ruff LSP server not properly configured"))
        } else {
            if (ruffConfigService.useRuffFormat) {
                super.setUserEnvironmentVariables(mapOf("RUFF_EXPERIMENTAL_FORMATTER" to "1"))
            }
            super.setWorkingDirectory(project.basePath)
            super.setCommands(commands)
        }
    }

    private fun createCommand(project: Project, ruffConfigService: RuffConfigService): List<String>? {
        if (!ruffConfigService.enableLsp) {
            RuffLoggingService.log(
                project,
                "LSP is not enabled in Ruff configuration",
                ConsoleViewContentType.ERROR_OUTPUT
            )
            return null
        }

        if (!ruffConfigService.useRuffLsp && !ruffConfigService.useRuffServer) {
            RuffLoggingService.log(
                project,
                "Neither Ruff LSP nor Ruff Server is enabled",
                ConsoleViewContentType.ERROR_OUTPUT
            )
            return null
        }

        if (!ruffConfigService.useLsp4ij) {
            RuffLoggingService.log(
                project,
                "LSP4IJ is not enabled in Ruff configuration",
                ConsoleViewContentType.ERROR_OUTPUT
            )
            return null
        }

        if (!isInspectionEnabled(project)) {
            RuffLoggingService.log(project, "Ruff inspection is not enabled", ConsoleViewContentType.ERROR_OUTPUT)
            return null
        }

        val ruffCacheService = RuffCacheService.getInstance(project)

        try {
            val (ruffFile, args) = when {
                ruffConfigService.useRuffServer && ruffCacheService.hasLsp() == true -> {
                    val executable = getRuffExecutable(project, ruffConfigService, false, ruffCacheService)
                    if (executable == null) {
                        RuffLoggingService.log(
                            project,
                            "Ruff executable not found",
                            ConsoleViewContentType.ERROR_OUTPUT
                        )
                        return null
                    }
                    executable to project.LSP_ARGS + (getConfigArgs(ruffConfigService) ?: listOf())
                }

                else -> {
                    val executable = getRuffExecutable(project, ruffConfigService, true, ruffCacheService)
                    if (executable == null) {
                        RuffLoggingService.log(
                            project,
                            "Ruff-LSP executable not found",
                            ConsoleViewContentType.ERROR_OUTPUT
                        )
                        return null
                    }
                    executable to listOf()
                }
            }

            val commandLine = getGeneralCommandLine(ruffFile, project, *args.toTypedArray())
            if (commandLine == null) {
                RuffLoggingService.log(project, "Failed to create command line", ConsoleViewContentType.ERROR_OUTPUT)
                return null
            }

            val commands = commandLine.getCommandLineList(null)
            RuffLoggingService.log(project, "LSP command: ${commands.joinToString(" ")}")
            return commands
        } catch (e: Exception) {
            RuffLoggingService.log(
                project,
                "Error creating LSP command: ${e.message}",
                ConsoleViewContentType.ERROR_OUTPUT
            )
            return null
        }
    }
}