package com.koxudaxi.ruff

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.jetbrains.python.psi.PyUtil


abstract class RuffPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement = source
    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        if (!isEnabled(source.project)) return TextRange.EMPTY_RANGE
        if (!source.isApplicableTo) return TextRange.EMPTY_RANGE

        val sourceFile = source.sourceFile
        val formatted = executeOnPooledThread(null) {
            process(sourceFile)
        } ?: return TextRange.EMPTY_RANGE
        val sourceDiffRange = diffRange(sourceFile.text, formatted) ?: return TextRange.EMPTY_RANGE

        val formattedDiffRange = diffRange(formatted, sourceFile.text) ?: return TextRange.EMPTY_RANGE
        val formattedPart = formatted.substring(formattedDiffRange.startOffset, formattedDiffRange.endOffset)

        return PyUtil.updateDocumentUnblockedAndCommitted<TextRange>(
            source
        ) { document: Document ->
            // Verify that the document has not been modified since the reformatting started
            if (!sourceFile.hasSameContentAsDocument(document)) return@updateDocumentUnblockedAndCommitted TextRange.EMPTY_RANGE
            document.replaceString(
                sourceDiffRange.startOffset,
                sourceDiffRange.endOffset,
                formattedPart
            )
            sourceDiffRange
        } ?: TextRange.EMPTY_RANGE
    }

    abstract fun isEnabled(project: Project): Boolean
    abstract fun process(sourceFile: SourceFile): String?

    private fun diffRange(s1: String, s2: String): TextRange? {
        if (s1 == s2) return null
        if (s2.isEmpty()) return TextRange(0, s1.length)
        val minLength = minOf(s1.length, s2.length)
        val start = s1.zip(s2).indexOfFirst { pair -> pair.first != pair.second }.let { if (it == -1) minLength else it }

        val relativeEnd = (1..minLength)
            .indexOfFirst { s1[s1.length - it] != s2[s2.length - it] }.let { if (it == -1) minLength else it - 1}
        val end = s1.length - relativeEnd
        return TextRange(start, end)
    }
}