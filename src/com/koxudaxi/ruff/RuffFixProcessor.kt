package com.koxudaxi.ruff

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile


class RuffFixProcessor : RuffPostFormatProcessor() {
    override fun isEnabled(element: PsiElement): Boolean =
        RuffConfigService.getInstance(element.project).runRuffOnReformatCode
    override fun process(pyFile: PsiFile): String? = fix(pyFile)
}