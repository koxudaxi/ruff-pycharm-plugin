package com.koxudaxi.ruff

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.jetbrains.python.PythonLanguage

class RuffNoqaDocumentationTargetProvider : DocumentationTargetProvider {
    override fun documentationTargets(file: PsiFile, offset: Int): MutableList<out DocumentationTarget> {
        if (!file.language.isKindOf(PythonLanguage.INSTANCE)) return mutableListOf()
        val psiComment = file.findElementAt(offset) as? PsiComment ?: return mutableListOf()
        return mutableListOf(RuffNoqaDocumentationTarget(psiComment, psiComment, offset))
    }
}
