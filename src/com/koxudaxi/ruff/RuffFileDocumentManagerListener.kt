package com.koxudaxi.ruff

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

class RuffFileDocumentManagerListener(project: Project) : FileDocumentManagerListener {
    private val ruffConfigService = RuffConfigService.getInstance(project)
    private val ruffFixService by lazy { RuffFixService.getInstance(project)}
    private val psiDocumentManager by lazy { PsiDocumentManager.getInstance(project) }
    override fun beforeDocumentSaving(document: Document) {
        if (!ruffConfigService.runRuffOnSave) return
        val psiFile = psiDocumentManager.getPsiFile(document) ?: return
        if (!psiFile.isApplicableTo) return
        ruffFixService.fix(document, psiFile)
    }
}