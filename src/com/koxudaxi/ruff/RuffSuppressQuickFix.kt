package com.koxudaxi.ruff

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyPsiUtils
import java.util.regex.Pattern

class RuffSuppressQuickFix(
    private val code: String,
    private val message: String,
    private val textRange: TextRange,
    private val lineOffset: Int,
) :
    LocalQuickFix {

    private val NOQA_COMMENT_PATTERN = Pattern.compile(
        "# noqa(?::[\\s]?(?<codes>([A-Z]+[0-9]+(?:[,\\s]+)?)+))?.*",
        Pattern.CASE_INSENSITIVE
    )
    data class NoqaCodes(val codes: List<String>?, val noqaStartOffset: Int, val noqaEndOffset: Int)
    private fun extractNoqaCodes(comment: PsiComment): NoqaCodes? {
        val commentText = comment.text ?: return null
        val noqaOffset = commentText.lowercase().indexOf("# noqa")
        val noqaStartOffset = comment.textRange.startOffset + noqaOffset
        val matcher = NOQA_COMMENT_PATTERN.matcher(commentText.substring(noqaOffset))
        if (!matcher.find()) {
            return NoqaCodes(null, noqaStartOffset, noqaStartOffset + "# noqa".length)
        }
        val codes = matcher.group("codes")?.split("[,\\s]+".toRegex())?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()
        return NoqaCodes(codes, noqaStartOffset,noqaStartOffset + matcher.end())
    }

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
            val prefix = if (startOffset != null &&  document.text[startOffset -1] == ' ') ""  else " "
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