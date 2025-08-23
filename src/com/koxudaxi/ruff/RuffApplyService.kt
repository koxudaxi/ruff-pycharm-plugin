package com.koxudaxi.ruff

import com.intellij.credentialStore.toByteArrayAndClear
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import kotlin.io.path.Path


@Service(Service.Level.PROJECT)
class RuffApplyService(val project: Project) {
    private val undoManager by lazy { UndoManager.getInstance(project) }
    private val psiDocumentManager by lazy { PsiDocumentManager.getInstance(project) }

    fun apply(psiFile: PsiFile, withFormat: Boolean) {
        if (project.isDisposed) return
        val document = psiDocumentManager.getDocument(psiFile) ?: return
        apply(document, psiFile.sourceFile, withFormat)
    }

    private fun write(document: Document, sourceFile: SourceFile, formatted: String) {
        if (formatted.isEmpty() || project.isDisposed) return

        val projectRef = project
        runInEdt {
            if (projectRef.isDisposed) return@runInEdt

            runWriteAction {
                if (projectRef.isDisposed) return@runWriteAction

                CommandProcessor.getInstance().runUndoTransparentAction {
                    try {
                        if (!undoManager.isUndoInProgress &&
                            !projectRef.isDisposed &&
                            sourceFile.hasSameContentAsDocument(document)) {
                            document.setText(formatted)
                        }
                    } catch (e: Exception) {
                        // Silent error handling
                    }
                }
            }
        }
    }

    val actionName = "Formatting with Ruff"

    fun apply(document: Document, sourceFile: SourceFile, withFormat: Boolean) {
        if (project.isDisposed) return

        try {
            val projectRef = project

            val configPath = if (projectRef.configService.useClosestConfig)
                findClosestRuffConfig(sourceFile)
            else
                null

            val fixArgs = if (configPath != null)
                projectRef.FIX_ARGS + listOf("--config", configPath)
            else
                projectRef.FIX_ARGS

            val checkedFixed = runReadActionOnPooledThread(projectRef,null) {
                if (projectRef.isDisposed) return@runReadActionOnPooledThread null
                RuffLoggingService.log(projectRef, "Applying Ruff fix to ${sourceFile.name}")

                try {
                    val fixed = runRuff(sourceFile, fixArgs)
                    if (projectRef.isDisposed) return@runReadActionOnPooledThread null
                    checkFixResult(sourceFile, fixed)
                } catch (e: Exception) {
                    RuffLoggingService.log(projectRef, "Error applying fix: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
                    null
                }
            }

            if (projectRef.isDisposed) return

            if (!withFormat) {
                if (checkedFixed is String && checkedFixed.isNotEmpty()) {
                    write(document, sourceFile, checkedFixed)
                }
                return
            }

            val sourceByte = when {
                checkedFixed is String && checkedFixed.isNotEmpty() -> checkedFixed.toCharArray().toByteArrayAndClear()
                else -> sourceFile.asStdin
            } ?: return

            val formatCommandArgs = generateCommandArgs(
                sourceFile.project,
                sourceByte,
                FORMAT_ARGS,
                true,
                configPath = configPath
            ) ?: return

            if (projectRef.isDisposed) return
            RuffLoggingService.log(projectRef, "Applying Ruff format to ${sourceFile.name}")

            val formatResult = runReadActionOnPooledThread(projectRef,null) {
                if (projectRef.isDisposed) return@runReadActionOnPooledThread null

                try {
                    val formatted = formatCommandArgs.let { runRuff(it) }
                    if (projectRef.isDisposed) return@runReadActionOnPooledThread null
                    checkFormatResult(sourceFile, formatted)
                } catch (e: Exception) {
                    RuffLoggingService.log(projectRef, "Error applying format: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
                    null
                }
            }

            if (projectRef.isDisposed) return

            if (formatResult is String && formatResult.isNotEmpty()) {
                write(document, sourceFile, formatResult)
            }
        } catch (e: Exception) {
            RuffLoggingService.log(project, "Unexpected error in apply: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
        }
    }

    companion object {
        fun getInstance(project: Project): RuffApplyService {
            return project.getService(RuffApplyService::class.java)
        }
    }
}

fun findClosestRuffConfig(sourceFile: SourceFile): String? {
    val virtualFile = sourceFile.virtualFile ?: return null
    val configFiles = listOf("pyproject.toml", "ruff.toml")
    
    return generateSequence(Path(virtualFile.path)) { it.parent }
        .firstNotNullOfOrNull { dir ->
            configFiles
                .map { dir.resolve(it) }
                .firstOrNull { it.toFile().exists() }
                ?.toString()
        }
}