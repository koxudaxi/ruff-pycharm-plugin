package com.koxudaxi.ruff

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyUtil

class RuffSuppressQuickFix(
    private val code: String,
    private val message: String,
    private val offset: Int) :
    LocalQuickFix {


    override fun getFamilyName(): String =
        "Suppress \"$code ${message}\""

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        PyUtil.updateDocumentUnblockedAndCommitted(
            descriptor.psiElement.containingFile
        ) { document: Document ->
                document.replaceString(
                    offset,
                    offset,
                    " # noqa: $code"
                )
        }
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