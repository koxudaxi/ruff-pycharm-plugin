package com.koxudaxi.ruff

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

class RuffFileDocumentManagerListener(private val project: Project) : FileDocumentManagerListener {
    private val undoManager = UndoManager.getInstance(project)
    private val ruffConfigService = RuffConfigService.getInstance(project)
    private val psiDocumentManager = PsiDocumentManager.getInstance(project)
    private val args = listOf("--exit-zero", "--no-cache", "--fix",  "-")
    override fun beforeDocumentSaving(document: Document) {
        if (!ruffConfigService.runRuffOnSave) return
        val psiFile = psiDocumentManager.getPsiFile(document) ?: return
        if (!psiFile.isApplicableTo) return
        val fileName = psiFile.name

        val stdin = document.text.toByteArray(psiFile.virtualFile.charset)
        runRuffInBackground(project, stdin, args, "running ruff $fileName") {
            if (it !is String) return@runRuffInBackground
            runInEdt {
                runWriteAction {
                    CommandProcessor.getInstance().executeCommand(
                        project,
                        {
                            if (!undoManager.isUndoInProgress) {
                                document.setText(it)
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
    }
}