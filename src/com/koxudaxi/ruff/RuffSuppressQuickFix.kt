package com.koxudaxi.ruff

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.jetbrains.python.inspections.flake8.Flake8InspectionSuppressor
import com.jetbrains.python.psi.PyUtil

class RuffSuppressQuickFix(
    private val code: String,
    private val message: String,
    private val offset: Int
) :
    LocalQuickFix {


    override fun getFamilyName(): String =
        "Suppress \"$code ${message}\""

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val psiFile = descriptor.psiElement.containingFile
        val comment = PyUtil.`as`(
            psiFile.findElementAt(offset),
            PsiComment::class.java
        )

        val noqaCode = (comment?.let {
            Flake8InspectionSuppressor.extractNoqaCodes(it)

        } ?: emptySet()) + setOf(code)
//        if (comment != null) {
//            comment.textRangeInParent
//        }
        PyUtil.updateDocumentUnblockedAndCommitted(psiFile) { document: Document ->
            document.replaceString(
                offset,
                offset,
                " # noqa: ${noqaCode.joinToString(separator = ", ")}"
            )
        }
    }

    private fun getNoqaCodes(psiFile: PsiFile, lineLastOffset: Int): Set<String> {
        val comment = PyUtil.`as`(
            psiFile.findElementAt(lineLastOffset),
            PsiComment::class.java
        )
        return comment?.let { Flake8InspectionSuppressor.extractNoqaCodes(it) } ?: emptySet()
    }

    companion object {
        fun create(result: Result, document: Document): RuffSuppressQuickFix {
            return RuffSuppressQuickFix(
                result.code,
                result.message,
                document.getLineEndOffset(result.location.row - 1)
            )
        }
    }
}