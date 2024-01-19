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
        if (!isEnabled(source.project)) return rangeToReformat
        if (!source.isApplicableTo) return rangeToReformat

        val sourceFile = source.getSourceFile(rangeToReformat, true)
        val text = sourceFile.text ?: return rangeToReformat
        val formatted = executeOnPooledThread(null) {
            process(sourceFile)
        } ?: return rangeToReformat
        if (text == formatted) return rangeToReformat
        val sourceDiffRange = diffRange(text, formatted) ?: return rangeToReformat

        return PyUtil.updateDocumentUnblockedAndCommitted<TextRange>(
            source
        ) { document: Document ->
            // Verify that the document has not been modified since the reformatting started
            if (!sourceFile.hasSameContentAsDocument(document)) return@updateDocumentUnblockedAndCommitted rangeToReformat

            // document.replaceString(startOffset, endOffset, formattedPart) does not keep the line break point markers and caret position
            document.setText(formatted)

            val endOffset = rangeToReformat.startOffset + sourceDiffRange.length
            when {
                endOffset <= rangeToReformat.endOffset -> rangeToReformat
                else -> TextRange.create(rangeToReformat.startOffset, endOffset)
            }
        } ?: rangeToReformat
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