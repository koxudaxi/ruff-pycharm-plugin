package com.koxudaxi.ruff

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.jetbrains.python.psi.PyUtil

class RuffQuickFix(private val content: String, private val range: TextRange, private val message: String?) :
    LocalQuickFix {
    override fun getFamilyName(): String =
        when (message) {
            is String -> message
            else -> DEFAULT_FIX_MESSAGE
        }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        PyUtil.updateDocumentUnblockedAndCommitted(
            descriptor.psiElement.containingFile
        ) { document: Document ->
            document.replaceString(
                range.startOffset,
                range.endOffset,
                content
            )
        }
    }

    companion object {
        private const val DEFAULT_FIX_MESSAGE = "Quick fix with ruff"
        fun create(fix: Fix, document: Document): RuffQuickFix {
            return RuffQuickFix(fix.content, document.getStartEndRange(fix.location, fix.endLocation, 0), fix.message)
        }
    }
}