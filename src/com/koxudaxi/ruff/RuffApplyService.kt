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
        runInEdt {
            runWriteAction {
                CommandProcessor.getInstance().runUndoTransparentAction {
                    if (!undoManager.isUndoInProgress && sourceFile.hasSameContentAsDocument(document)) {
                        document.setText(formatted)
                    }
                }
            }
        }
    }

    fun apply(document: Document, sourceFile: SourceFile, withFormat: Boolean) {
        val checkedFixed = runReadActionOnPooledThread(null) {
            val fixed = runRuff(sourceFile, FIX_ARGS)
            checkFixResult(sourceFile, fixed)

        }

        if (!withFormat) {
            if (checkedFixed is String) {
                write(document, sourceFile, checkedFixed)
            }
            return
        }

        val sourceByte = when {
            checkedFixed is String -> checkedFixed.toCharArray().toByteArrayAndClear()
            else -> sourceFile.asStdin
        }
        val formatCommandArgs = generateCommandArgs(
                sourceFile.project,
                sourceByte,
                FORMAT_ARGS
        )
        val formatResult = runReadActionOnPooledThread(null) {
            val formatted = formatCommandArgs?.let { runRuff(it) }
            checkFormatResult(sourceFile, formatted)
        } ?: return
        write(document, sourceFile, formatResult)
    }

    companion object {
        fun getInstance(project: Project): RuffApplyService {
            return project.getService(RuffApplyService::class.java)
        }
    }
}