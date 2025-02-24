package com.koxudaxi.ruff

import com.intellij.credentialStore.toByteArrayAndClear
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
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

            val checkedFixed = runReadActionOnPooledThread(null) {
                if (projectRef.isDisposed) return@runReadActionOnPooledThread null

                try {
                    val fixed = runRuff(sourceFile, projectRef.FIX_ARGS)
                    if (projectRef.isDisposed) return@runReadActionOnPooledThread null
                    checkFixResult(sourceFile, fixed)
                } catch (e: Exception) {
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
                FORMAT_ARGS
            ) ?: return

            if (projectRef.isDisposed) return

            val formatResult = runReadActionOnPooledThread(null) {
                if (projectRef.isDisposed) return@runReadActionOnPooledThread null

                try {
                    val formatted = formatCommandArgs.let { runRuff(it) }
                    if (projectRef.isDisposed) return@runReadActionOnPooledThread null
                    checkFormatResult(sourceFile, formatted)
                } catch (e: Exception) {
                    null
                }
            }

            if (projectRef.isDisposed) return

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