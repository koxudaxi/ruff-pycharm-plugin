package com.koxudaxi.ruff.lsp.lsp4ij

import RuffLspClientManager
import com.google.gson.JsonObject
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.koxudaxi.ruff.RuffConfigService
import com.koxudaxi.ruff.RuffLoggingService
import com.koxudaxi.ruff.lsp.ClientType
import com.koxudaxi.ruff.lsp.lsp4ij.RuffLsp4IntellijClient.Companion.LANGUAGE_SERVER_ID
import com.redhat.devtools.lsp4ij.LSPIJUtils
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import org.eclipse.lsp4j.ExecuteCommandParams
import java.util.concurrent.CompletableFuture

class RuffLanguageClient(private val project: Project) : LanguageClientImpl(project) {
    /**
     * Executes multiple Ruff commands sequentially based on configuration
     *
     * @param file The virtual file to execute commands on
     * @return A CompletableFuture that completes when all actions are executed
     */
    fun executeMultipleCommands(file: VirtualFile): CompletableFuture<Nothing?> {
        RuffLoggingService.log(project, "Starting sequential command execution for file: ${file.name}")

        val config = RuffConfigService.getInstance(project)

        var future = CompletableFuture.completedFuture<Any?>(null)

        if (config.codeActionFeature) {
            future = future.thenCompose { _ -> doFixAll(file) }
        }

        // Apply formatting if formattingFeature and useRuffFormat are enabled
        if (config.formattingFeature && config.useRuffFormat) {
            future = future.thenCompose { _ -> doFormat(file) }
        }

        if (config.codeActionFeature) {
            future = future.thenCompose { _ -> doOrganizeImports(file) }
        }

        return future.thenApply { null }
            .exceptionally { e ->
                RuffLoggingService.log(
                    project,
                    "Error executing sequential commands: ${e.message}",
                    ConsoleViewContentType.ERROR_OUTPUT
                )
                null
            }
    }

    /**
     * Execute source.fixall code action for the given document
     *
     * @param file The virtual file to execute fixall on
     * @return A CompletableFuture that completes when the action is executed
     */
    fun doFixAll(file: VirtualFile): CompletableFuture<Any?> {
        RuffLoggingService.log(project, "Starting Fix All operation for file: ${file.name}")
        return executeCommand(file, "ruff.applyAutofix")
    }

    /**
     * Execute format document command for the given document
     *
     * @param file The virtual file to format
     * @return A CompletableFuture that completes when the action is executed
     */
    private fun doFormat(file: VirtualFile): CompletableFuture<Any?> {
        RuffLoggingService.log(project, "Starting Format operation for file: ${file.name}")
        return executeCommand(file, "ruff.applyFormat")
    }

    /**
     * Execute organize imports command for the given document
     *
     * @param file The virtual file to organize imports
     * @return A CompletableFuture that completes when the action is executed
     */
    private fun doOrganizeImports(file: VirtualFile): CompletableFuture<Any?> {
        RuffLoggingService.log(project, "Starting Organize Imports operation for file: ${file.name}")
        return executeCommand(file, "ruff.applyOrganizeImports")
    }

    /**
     * Common method to execute LSP commands
     *
     * @param file The virtual file to execute command on
     * @param command The command identifier to execute
     * @return A CompletableFuture that completes when the command is executed
     */
    private fun executeCommand(file: VirtualFile, command: String): CompletableFuture<Any?> {
        // Get language server manager
        val lsManager = RuffLspClientManager.getInstance(project)
            .getClient(ClientType.LSP4IJ)
            ?.getLanguageServerManager() as? LanguageServerManager

        if (lsManager == null) {
            RuffLoggingService.log(project, "Language server manager not found", ConsoleViewContentType.ERROR_OUTPUT)
            return CompletableFuture.completedFuture(null)
        }

        val serverFuture = lsManager.getLanguageServer(LANGUAGE_SERVER_ID)
        val languageServer = try {
            serverFuture.get()
        } catch (e: Exception) {
            RuffLoggingService.log(
                project,
                "Error getting language server: ${e.message}",
                ConsoleViewContentType.ERROR_OUTPUT
            )
            return CompletableFuture.completedFuture(null)
        }

        if (languageServer == null) {
            RuffLoggingService.log(project, "Language server not found", ConsoleViewContentType.ERROR_OUTPUT)
            return CompletableFuture.completedFuture(null)
        }

        val textDocumentIdentifier = LSPIJUtils.toTextDocumentIdentifier(file)
        val document = LSPIJUtils.getDocument(file)

        if (document == null) {
            RuffLoggingService.log(
                project,
                "Could not get document for file: ${file.name}",
                ConsoleViewContentType.ERROR_OUTPUT
            )
            return CompletableFuture.completedFuture(null)
        }

        val fileUri = textDocumentIdentifier.uri
        RuffLoggingService.log(project, "Executing command: $command for file URI: $fileUri")

        val argument = JsonObject()
        argument.addProperty("uri", fileUri)
        argument.addProperty("version", document.modificationStamp)

        val executeParams = ExecuteCommandParams(command, listOf(argument))

        return languageServer.workspaceService.executeCommand(executeParams)
            .exceptionally { e ->
                RuffLoggingService.log(
                    project,
                    "Error executing $command: ${e.message}",
                    ConsoleViewContentType.ERROR_OUTPUT
                )
                null
            }
    }
}