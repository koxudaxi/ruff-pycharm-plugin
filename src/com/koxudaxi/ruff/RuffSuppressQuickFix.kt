package com.koxudaxi.ruff

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyPsiUtils

class RuffSuppressQuickFix(
    private val code: String,
    private val message: String,
    private val textRange: TextRange,
    private val lineOffset: Int,
) :
    LocalQuickFix {


    override fun getFamilyName(): String =
        "Suppress \"$code ${message}\""

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val psiFile = descriptor.psiElement.containingFile
        val comment = PyUtil.`as`(
            psiFile.findElementAt(textRange.startOffset),
            PsiElement::class.java
        )?.let { PyPsiUtils.findSameLineComment(it) }

        val (existingNoqaCodes, startOffset, endOffset ) = if (comment == null) {
            Triple(listOf(), null, null)
        } else {
            val noqaCodes = extractNoqaCodes(comment)
            if (noqaCodes?.codes == null) {
                Triple(listOf(), noqaCodes?.noqaStartOffset, noqaCodes?.noqaEndOffset)
            } else {
               Triple(noqaCodes.codes, noqaCodes.noqaStartOffset, noqaCodes.noqaEndOffset)
            }
        }

        PyUtil.updateDocumentUnblockedAndCommitted(psiFile) { document: Document ->
            val prefix = if (startOffset != null && document.text[startOffset -1] == ' ') ""  else "  "
            document.replaceString(
                startOffset ?: document.getLineEndOffset(lineOffset),
                endOffset ?: document.getLineEndOffset(lineOffset),
                "${prefix}# noqa: ${(existingNoqaCodes + code).joinToString(separator = ", ")}"
            )
        }
    }

    companion object {
        fun create(result: Result, document: Document): RuffSuppressQuickFix {
            return RuffSuppressQuickFix(
                result.code,
                result.message,
                document.getStartEndRange(result.location, result.endLocation, 0),
                result.location.row - 1
            )
        }
    }
}
