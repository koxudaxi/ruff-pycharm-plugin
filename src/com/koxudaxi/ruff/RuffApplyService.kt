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
                CommandProcessor.getInstance().executeCommand(
                    project,
                    {
                        if (!undoManager.isUndoInProgress && sourceFile.hasSameContentAsDocument(document)) {
                            document.setText(formatted)
                        }
                    },
                    "Run ruff",
                    null,
                    UndoConfirmationPolicy.DEFAULT,
                    document
                )
            }
        }
    }

    fun apply(document: Document, sourceFile: SourceFile, withFormat: Boolean) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val fixed = runRuff(sourceFile, FIX_ARGS)
            val checkedFixed = checkFixResult(sourceFile, fixed)

            if (!withFormat) {
                if (checkedFixed is String) {
                    write(document, sourceFile, checkedFixed)
                }
                return@executeOnPooledThread
            }

            val sourceByte = when {
                checkedFixed is String -> checkedFixed.toCharArray().toByteArrayAndClear()
                else -> sourceFile.asStdin
            }
            val formatted = generateCommandArgs(
                sourceFile.project,
                sourceByte,
                FORMAT_ARGS
            )?.let { runRuff(it) }

            checkFormatResult(sourceFile, formatted)?.let {
                write(document, sourceFile, it)
            }
        }
    }

    companion object {
        fun getInstance(project: Project): RuffApplyService {
            return project.getService(RuffApplyService::class.java)
        }
    }
}