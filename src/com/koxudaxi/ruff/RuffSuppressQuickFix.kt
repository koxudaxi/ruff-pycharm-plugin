package com.koxudaxi.ruff

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyUtil

class RuffSuppressQuickFix(private val edits: Edit) :
    LocalQuickFix {
    data class Edit(
        val code: String,
        val message: String,
        val offset: Int,
    )

    override fun getFamilyName(): String =
        "Suppress ${edits.code} ${edits.message}"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        PyUtil.updateDocumentUnblockedAndCommitted(
            descriptor.psiElement.containingFile
        ) { document: Document ->
                document.replaceString(
                    edits.offset,
                    edits.offset,
                    " # noqa: ${edits.code}"
                )
        }
    }

    companion object {
        fun create(result: Result, document: Document): RuffSuppressQuickFix {
            return RuffSuppressQuickFix(Edit(
                result.code,
                result.message,
                document.getLineEndOffset(result.location.row - 1)
            ))
        }
    }
}