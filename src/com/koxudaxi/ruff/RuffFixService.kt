package com.koxudaxi.ruff

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
class RuffFixService(val project: Project) {
    private val undoManager by lazy { UndoManager.getInstance(project) }
    private val psiDocumentManager by lazy { PsiDocumentManager.getInstance(project) }

    fun fix(psiFile: PsiFile) {
        val document = psiDocumentManager.getDocument(psiFile) ?: return
        fix(document, psiFile.sourceFile)
    }

    fun fix(document: Document, sourceFile: SourceFile) {
        runRuffInBackground(sourceFile, FIX_ARGS) {
            val formatted = checkFixResult(sourceFile, it) ?: return@runRuffInBackground
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
    }
    companion object {

        fun getInstance(project: Project): RuffFixService {
            return project.getService(RuffFixService::class.java)
        }
    }

}