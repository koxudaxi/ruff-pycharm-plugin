package com.koxudaxi.ruff

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.lang.Language
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile

class RuffNoqaDocumentationTargetProvider : DocumentationTargetProvider {
    override fun documentationTargets(file: PsiFile, offset: Int): MutableList<out DocumentationTarget> {
        val pythonLanguage = Language.findLanguageByID("Python") ?: return mutableListOf()
        if (!file.language.isKindOf(pythonLanguage)) return mutableListOf()
        val psiComment = file.findElementAt(offset) as? PsiComment ?: return mutableListOf()
        return mutableListOf(RuffNoqaDocumentationTarget(psiComment, psiComment, offset))
    }
}
