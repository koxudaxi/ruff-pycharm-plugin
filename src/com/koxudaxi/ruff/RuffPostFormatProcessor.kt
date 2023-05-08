package com.koxudaxi.ruff

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.jetbrains.python.psi.PyUtil


class RuffPostFormatProcessor : PostFormatProcessor {
    private val argsBase = listOf("--exit-zero", "--no-cache", "--force-exclude")
    private val fixArgs = argsBase + listOf("--fix")
    private val noFixArgs = argsBase + listOf("--no-fix", "--format", "json")
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement = source


    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        if (!RuffConfigService.getInstance(source.project).runRuffOnReformatCode) return TextRange.EMPTY_RANGE
        val pyFile = source.containingFile
        if (!pyFile.isApplicableTo) return TextRange.EMPTY_RANGE

        val formatted = executeOnPooledThread(null) {
            format(pyFile)
        } ?: return TextRange.EMPTY_RANGE
        val sourceDiffRange = diffRange(source.text, formatted) ?: return TextRange.EMPTY_RANGE

        val formattedDiffRange = diffRange(formatted, source.text) ?: return TextRange.EMPTY_RANGE
        val formattedPart = formatted.substring(formattedDiffRange.startOffset, formattedDiffRange.endOffset)

        return PyUtil.updateDocumentUnblockedAndCommitted<TextRange>(
            pyFile
        ) { document: Document ->
            document.replaceString(
                sourceDiffRange.startOffset,
                sourceDiffRange.endOffset,
                formattedPart
            )
            sourceDiffRange
        } ?: TextRange.EMPTY_RANGE
    }
    private fun format(pyFile: PsiFile): String?  {
        val fixResult = runRuff(pyFile, fixArgs) ?: return null
        if (fixResult.isNotBlank()) return fixResult
        val noFixResult = runRuff(pyFile, noFixArgs) ?: return null

        // check the file is excluded
        if (noFixResult == "[]\n") return null
        return fixResult
    }

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