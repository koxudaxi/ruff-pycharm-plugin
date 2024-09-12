package com.koxudaxi.ruff

import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener.ActionOnSave
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

class RuffActionOnSave : ActionOnSave() {
    override fun isEnabledForProject(project: Project): Boolean {
        val ruffConfigService = RuffConfigService.getInstance(project)
        return ruffConfigService.runRuffOnSave
    }
    override fun processDocuments(project: Project, documents: Array<Document>) {
        val ruffApplyService = RuffApplyService.getInstance(project)
        val ruffConfigService = RuffConfigService.getInstance(project)
        val ruffCacheService = RuffCacheService.getInstance(project)
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        val withFormat = ruffConfigService.useRuffFormat && ruffCacheService.hasFormatter()
        for (document in documents) {
            val psiFile = psiDocumentManager.getPsiFile(document) ?: continue
            if (!psiFile.isApplicableTo) continue
            if (ruffConfigService.disableOnSaveOutsideOfProject && !psiFile.virtualFile.isInProjectDir(project)) return
            ruffApplyService.apply(document, psiFile.sourceFile, withFormat)
        }
    }
}