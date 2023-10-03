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

    internal fun diffRange(source: String, target: String): TextRange? {
        if (source == target) return null
        if (source.isEmpty() || target.isEmpty()) return TextRange(0, source.length)

        val start = source.zip(target).takeWhile { it.first == it.second }.count()

        var endSource = source.lastIndex
        var endTarget = target.lastIndex
        while (endSource >= start && endTarget >= start && source[endSource] == target[endTarget]) {
            endSource--
            endTarget--
        }
        return TextRange(start, endSource + 1)
    }
}