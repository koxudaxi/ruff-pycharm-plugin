package com.koxudaxi.ruff

import com.intellij.credentialStore.toByteArrayAndClear

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.jetbrains.python.psi.PyUtil


class RuffPostFormatProcessor : PostFormatProcessor {
    private val argsBase = listOf("--exit-zero", "--no-cache", "--fix")
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement = source


    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        if (!RuffConfigService.getInstance(source.project).runRuffOnReformatCode) return TextRange.EMPTY_RANGE
        val pyFile = source.containingFile
        if (!pyFile.isApplicableTo) return TextRange.EMPTY_RANGE

        val stdin = source.textToCharArray().toByteArrayAndClear()

        val formatted = executeOnPooledThread(null) {
            runRuff(pyFile.project, stdin, *(argsBase + getStdinFileNameArgs(source)).toTypedArray())
        } ?: return TextRange.EMPTY_RANGE
        val diffRange = diffRange(source.text, formatted) ?: return TextRange.EMPTY_RANGE

        val newPartEnd = formatted.length - (source.text.length - diffRange.endOffset)
        val newPart = formatted.substring(diffRange.startOffset, newPartEnd)

        return PyUtil.updateDocumentUnblockedAndCommitted<TextRange>(
            pyFile
        ) { document: Document ->
            document.replaceString(
                diffRange.startOffset,
                diffRange.endOffset,
                newPart
            )
            TextRange.from(diffRange.startOffset, newPart.length)
        } ?: TextRange.EMPTY_RANGE
    }

    private fun diffRange(s1: String, s2: String): TextRange? {
        if (s1 == s2) return null
        val start = s1.zip(s2).indexOfFirst { pair -> pair.first != pair.second }
        val relativeEnd = (1..minOf(s1.length, s2.length))
            .indexOfFirst { s1[s1.length - it] != s2[s2.length - it] }
        val end = s1.length - relativeEnd + 2
        return TextRange(start, end)
    }
}