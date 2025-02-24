package com.koxudaxi.ruff

import com.intellij.credentialStore.toByteArrayAndClear
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile


@Service(Service.Level.PROJECT)
class RuffApplyService(val project: Project) {
    private val undoManager by lazy { UndoManager.getInstance(project) }
    private val psiDocumentManager by lazy { PsiDocumentManager.getInstance(project) }

    fun apply(psiFile: PsiFile, withFormat: Boolean) {
        val document = psiDocumentManager.getDocument(psiFile) ?: return
        apply(document, psiFile.sourceFile, withFormat)
    }

    private fun write(document: Document, sourceFile: SourceFile, formatted: String) {
        if (formatted.isEmpty()) return

        runInEdt {
            runWriteAction {
                CommandProcessor.getInstance().runUndoTransparentAction {
                    try {
                        if (!undoManager.isUndoInProgress && sourceFile.hasSameContentAsDocument(document)) {
                            document.setText(formatted)
                        }
                    } catch (e: Exception) {
                        RuffLoggingService.log(project, "Error writing to document: ${e.message}")
                    }
                }
            }
        }
    }
    val actionName = "Formatting with Ruff"
    fun apply(document: Document, sourceFile: SourceFile, withFormat: Boolean) {
        try {
            val checkedFixed = runReadActionOnPooledThread(null) {
                try {
                    val fixed = runRuff(sourceFile, project.FIX_ARGS)
                    checkFixResult(sourceFile, fixed)
                } catch (e: Exception) {
                    null
                }
            }

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
                FORMAT_ARGS
            ) ?: return

            val formatResult = runReadActionOnPooledThread(null) {
                try {
                    val formatted = formatCommandArgs.let { runRuff(it) }
                    checkFormatResult(sourceFile, formatted)
                } catch (e: Exception) {
                    null
                }
            }

            if (formatResult is String && formatResult.isNotEmpty()) {
                write(document, sourceFile, formatResult)
            }
        } catch (e: Exception) {
            // Silent error handling
        }
    }

    companion object {
        fun getInstance(project: Project): RuffApplyService {
            return project.getService(RuffApplyService::class.java)
        }
    }
}