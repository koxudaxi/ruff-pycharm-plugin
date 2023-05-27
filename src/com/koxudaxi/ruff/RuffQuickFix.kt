package com.koxudaxi.ruff

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.jetbrains.python.psi.PyUtil

class RuffQuickFix(private val edits: List<Edit>, val message: String?) :
    LocalQuickFix {
    data class Edit(
        val content: String,
        val range: TextRange,
    )

    override fun getFamilyName(): String =
        when (message) {
            is String -> message
            else -> DEFAULT_FIX_MESSAGE
        }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        PyUtil.updateDocumentUnblockedAndCommitted(
            descriptor.psiElement.containingFile
        ) { document: Document ->
            edits.forEach { edit ->
                document.replaceString(
                    edit.range.startOffset,
                    edit.range.endOffset,
                    edit.content
                )
            }

        }
    }

    companion object {
        private const val DEFAULT_FIX_MESSAGE = "Quick fix with ruff"
        fun create(fix: Fix, document: Document): RuffQuickFix? {
            return when {
                fix.edits != null -> {
                    var offset = if (fix.applicability != null) { -1 } else { 0 }
                    fix.edits.map {
                        val range = document.getStartEndRange(it.location, it.endLocation, offset)
                        offset = offset - range.length + it.content.length
                        Edit(it.content, range)
                    }
                }

                fix.location != null && fix.endLocation != null && fix.content != null -> listOf(
                    Edit(
                        fix.content,
                        document.getStartEndRange(fix.location, fix.endLocation, 0)
                    )
                )

                else -> null
            }?.let {
                RuffQuickFix(it, fix.message)
            }
        }
    }
}