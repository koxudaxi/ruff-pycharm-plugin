package com.koxudaxi.ruff

import com.intellij.codeInsight.navigation.targetPresentation
import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiComment
import com.intellij.refactoring.suggested.createSmartPointer
import com.intellij.refactoring.suggested.startOffset

import com.intellij.webSymbols.utils.HtmlMarkdownUtils

class RuffNoqaDocumentationTarget(val psiComment: PsiComment, private val originalElement: PsiComment?, private val offset: Int): DocumentationTarget {
    override fun computeDocumentationHint(): String? {
        return computeLocalDocumentation(psiComment, offset)
    }

    override fun computePresentation(): TargetPresentation {
        return targetPresentation(psiComment)
    }

    override val navigatable: Navigatable?
        get() = psiComment as? Navigatable

    private val replaceHtmlTags = listOf(
        "<h1>" to "<h2>",
        "</h1>" to "</h2>",
        "<h2>" to "<h3>",
        "</h2>" to "</h3>",
        "<h3>" to "<h4>",
        "</h3>" to "</h4>",
        "<h4>" to "<h5>",
        "</h4>" to "</h5>",
        "<h5>" to "<h6>",
        "</h5>" to "</h6>",
    ).reversed()
    override fun computeDocumentation(): DocumentationResult? {
        @Suppress("HardCodedStringLiteral") val markdown =
            computeLocalDocumentation(psiComment,  offset) ?: return null
        val html = HtmlMarkdownUtils.toHtml(markdown, true) ?: return null
        val downscaledHtml = replaceHtmlTags.fold(html) { acc, (from, to) ->
            acc.replace(from, to)
        }
        return DocumentationResult.documentation(downscaledHtml)
    }

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPtr = psiComment.createSmartPointer()
        val originalElementPtr = originalElement?.createSmartPointer()
        return Pointer {
            val element = elementPtr.dereference() ?: return@Pointer null
            RuffNoqaDocumentationTarget(element, originalElementPtr?.dereference(), offset)
        }
    }
    private fun computeLocalDocumentation(psiComment: PsiComment, offset: Int): String? {
        val noqaCodes = extractNoqaCodes(psiComment) ?: return null
        if (noqaCodes.codes?.isNotEmpty() != true) return null
        if (noqaCodes.noqaStartOffset + "# noqa: ".length > offset || noqaCodes.noqaEndOffset < offset) return null
        val targetOffset = offset - psiComment.startOffset
        val startPart = psiComment.text.substring(0, targetOffset).takeLastWhile { it.isLetterOrDigit() }
        val endPart = psiComment.text.substring(targetOffset).takeWhile { it.isLetterOrDigit() }
        val noqaCode = startPart + endPart
        return runRuff(psiComment.project, listOf("--explain", noqaCode))
    }}
