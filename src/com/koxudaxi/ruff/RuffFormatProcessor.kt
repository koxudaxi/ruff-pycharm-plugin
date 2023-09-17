package com.koxudaxi.ruff

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile


class RuffFormatProcessor : RuffPostFormatProcessor() {
    override fun isEnabled(element: PsiElement): Boolean {
        val ruffConfigService = RuffConfigService.getInstance(element.project)
        return ruffConfigService.runRuffOnReformatCode && ruffConfigService.useRuffFormat && RuffCacheService.hasFormatter(element.project)
    }
    override fun process(pyFile: PsiFile): String? =  format(pyFile)
}