package com.koxudaxi.ruff.lsp.lsp4ij

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.*
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider
import java.util.concurrent.atomic.AtomicBoolean

class RuffLanguageServer(project: Project) : ProcessStreamConnectionProvider() {
    private val isInitialized = AtomicBoolean(false)

    init {
        try {
            super.setWorkingDirectory(project.basePath)

            val ruffConfigService = project.configService
            RuffLoggingService.log(project, "Initializing RuffLanguageServer for LSP4IJ")

            if (ruffConfigService.useRuffFormat) {
                super.setUserEnvironmentVariables(mapOf("RUFF_EXPERIMENTAL_FORMATTER" to "1"))
            }

            val commands = try {
                createCommand(project, ruffConfigService)
            } catch (e: Exception) {
                RuffLoggingService.log(
                    project,
                    "Error generating LSP commands: ${e.message}",
                    ConsoleViewContentType.ERROR_OUTPUT
                )
                null
            }

            if (commands.isNullOrEmpty()) {
                val errorCmd = listOf(
                    "echo",
                    "Error: Ruff LSP configuration is invalid. Please check the Ruff plugin settings."
                )
                RuffLoggingService.log(
                    project,
                    "Using fallback error command due to missing LSP configuration",
                    ConsoleViewContentType.ERROR_OUTPUT
                )
                super.setCommands(errorCmd)

            } else {
                RuffLoggingService.log(
                    project,
                    "LSP4IJ server configured with commands: ${commands.joinToString(" ")}",
                    ConsoleViewContentType.NORMAL_OUTPUT
                )
                super.setCommands(commands)
                isInitialized.set(true)
            }
        } catch (e: Exception) {
            RuffLoggingService.log(
                project,
                "Error creating LSP4IJ server: ${e.message}",
                ConsoleViewContentType.ERROR_OUTPUT
            )

            if (!isInitialized.get()) {
                throw e
            }
        }
    }

    private fun createCommand(project: Project, ruffConfigService: RuffConfigService): List<String>? {
        if (!ruffConfigService.enableLsp) {
            RuffLoggingService.log(project, "LSP is not enabled in Ruff configuration", ConsoleViewContentType.ERROR_OUTPUT)
            return null
        }

        if (!ruffConfigService.useRuffLsp && !ruffConfigService.useRuffServer) {
            RuffLoggingService.log(project, "Neither Ruff LSP nor Ruff Server is enabled", ConsoleViewContentType.ERROR_OUTPUT)
            return null
        }

        if (!ruffConfigService.useLsp4ij) {
            RuffLoggingService.log(project, "LSP4IJ is not enabled in Ruff configuration", ConsoleViewContentType.ERROR_OUTPUT)
            return null
        }

        if (!isInspectionEnabled(project)) {
            RuffLoggingService.log(project, "Ruff inspection is not enabled", ConsoleViewContentType.ERROR_OUTPUT)
            return null
        }

        val ruffCacheService = RuffCacheService.getInstance(project)

        val version = ruffCacheService.getVersion() ?: ruffCacheService.getOrPutVersion().getNow(null)
        if (version == null) {
            RuffLoggingService.log(
                project,
                "Attempting to create LSP command without established version. Using default behavior.",
                ConsoleViewContentType.NORMAL_OUTPUT
            )
        }

        try {
            val executablePair = when {
                ruffConfigService.useRuffServer -> {
                    val executable = getRuffExecutable(project, ruffConfigService, false, ruffCacheService)
                    if (executable == null) {
                        RuffLoggingService.log(project, "Ruff executable not found", ConsoleViewContentType.ERROR_OUTPUT)
                        return null
                    }
                    executable to project.LSP_ARGS + (getConfigArgs(ruffConfigService) ?: listOf())
                }
                else -> {
                    val executable = getRuffExecutable(project, ruffConfigService, true, ruffCacheService)
                    if (executable == null) {
                        RuffLoggingService.log(project, "Ruff-LSP executable not found", ConsoleViewContentType.ERROR_OUTPUT)
                        return null
                    }
                    executable to listOf()
                }
            }

            val (ruffFile, args) = executablePair
            val commandLine = getGeneralCommandLine(ruffFile, project, *args.toTypedArray())
            if (commandLine == null) {
                RuffLoggingService.log(project, "Failed to create command line", ConsoleViewContentType.ERROR_OUTPUT)
                return null
            }

            val commands = commandLine.getCommandLineList(null)
            RuffLoggingService.log(project, "Created LSP command: ${commands.joinToString(" ")}")
            return commands
        } catch (e: Exception) {
            RuffLoggingService.log(project, "Error creating LSP command: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
            return null
        }
    }
}