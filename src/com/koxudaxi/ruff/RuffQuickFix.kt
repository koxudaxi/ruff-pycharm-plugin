package com.koxudaxi.ruff

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.jetbrains.python.psi.PyUtil

class RuffQuickFix(private val content: String, private val range: TextRange) :
    LocalQuickFix {
    override fun getFamilyName(): String = "Quick fix"

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
        fun create(fix: Fix, document: Document): RuffQuickFix {
            val start = document.getLineStartOffset(fix.location.row - 1) + fix.location.column
            val end = document.getLineStartOffset(fix.endLocation.row - 1) + fix.endLocation.column
            return RuffQuickFix(fix.content, TextRange(start, end))
        }
    }
}