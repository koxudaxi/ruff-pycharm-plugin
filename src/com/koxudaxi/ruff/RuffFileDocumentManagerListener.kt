package com.koxudaxi.ruff

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

class RuffFileDocumentManagerListener(val project: Project) : FileDocumentManagerListener {
    private val ruffApplyService by lazy { RuffApplyService.getInstance(project) }
    private val ruffConfigService by lazy { RuffConfigService.getInstance(project) }
    private val ruffCacheService by lazy { RuffCacheService.getInstance(project) }
    private val psiDocumentManager by lazy { PsiDocumentManager.getInstance(project) }

    override fun beforeDocumentSaving(document: Document) {
        if (!ruffConfigService.runRuffOnSave) return
        val psiFile = psiDocumentManager.getPsiFile(document) ?: return
        if (ruffConfigService.disableOnSaveOutsideOfProject && !psiFile.virtualFile.isInProjectDir(project)) return
        if (!psiFile.isApplicableTo) return
        val withFormat = ruffConfigService.useRuffFormat && ruffCacheService.hasFormatter()
        ruffApplyService.apply(document, psiFile.sourceFile, withFormat)
    }
}