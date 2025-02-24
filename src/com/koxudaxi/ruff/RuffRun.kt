package com.koxudaxi.ruff

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE


class RuffRun : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val psiFile = e.getData(PSI_FILE) ?: return
        if (!psiFile.isApplicableTo) return
        val project = psiFile.project
        val withFormat = project.configService.useRuffFormat && RuffCacheService.hasFormatter(project)
        RuffApplyService.getInstance(psiFile.project).apply(psiFile, withFormat)
    }

    init {
        templatePresentation.icon = AllIcons.Actions.Execute
    }

    companion object {
        const val actionID = "runRuff"
    }
}