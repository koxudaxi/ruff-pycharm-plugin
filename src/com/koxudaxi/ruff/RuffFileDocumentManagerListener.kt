package com.koxudaxi.ruff

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.utils.editor.getVirtualFile

class RuffFileDocumentManagerListener(val project: Project) : FileDocumentManagerListener {
    private val ruffConfigService = RuffConfigService.getInstance(project)
    private val ruffFixService by lazy { RuffFixService.getInstance(project)}
    private val psiDocumentManager by lazy { PsiDocumentManager.getInstance(project) }
      override fun beforeDocumentSaving(document: Document) {
        if (!ruffConfigService.runRuffOnSave) return
        val psiFile = psiDocumentManager.getPsiFile(document) ?: return
        if (ruffConfigService.disableOnSaveOutsideOfProject && !psiFile.virtualFile.isInProjectDir(project)) return
        if (!psiFile.isApplicableTo) return
        ruffFixService.fix(document, psiFile)
    }
}