package com.koxudaxi.ruff

import com.intellij.codeInsight.navigation.targetPresentation
import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.model.Pointer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiComment
import com.intellij.psi.createSmartPointer
import com.intellij.psi.util.startOffset


class RuffNoqaDocumentationTarget(private val psiComment: PsiComment, private val originalElement: PsiComment?, private val offset: Int): DocumentationTarget {
    @Suppress("UnstableApiUsage")
    override fun computePresentation(): TargetPresentation {
        return targetPresentation(psiComment)
    }

    private val replaceHtmlTags = listOf(
        "<h1>" to "<h3>",
        "</h1>" to "</h3>",
        "<h2>" to "<h4>",
        "</h2>" to "</h4>",
        "<h3>" to "<h5>",
        "</h3>" to "</h5>",
        "<h4>" to "<h6>",
        "</h4>" to "</h6>",
        "<h5>" to "<h6>",
        "</h5>" to "</h6>",
    ).reversed()
    override fun computeDocumentation(): DocumentationResult? {
        val noqaCode = getNoqaCode(psiComment, offset) ?: return null
        return DocumentationResult.asyncDocumentation {
            val markdown = runRuff(psiComment.project, psiComment.project.RULE_ARGS + noqaCode, true) ?: return@asyncDocumentation null
            ApplicationManager.getApplication().runReadAction(Computable {
                val html = DocMarkdownToHtmlConverter.convert(psiComment.project, markdown) ?: return@Computable null
                val downscaledHtml = replaceHtmlTags.fold(html) { acc, (from, to) ->
                    acc.replace(from, to)
                }
                DocumentationResult.documentation(downscaledHtml)
            })
        }
    }

    @Suppress("UnstableApiUsage")
    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPtr = psiComment.createSmartPointer()
        val originalElementPtr = originalElement?.createSmartPointer()
        return Pointer {
            val element = elementPtr.dereference() ?: return@Pointer null
            RuffNoqaDocumentationTarget(element, originalElementPtr?.dereference(), offset)
        }
    }
    private fun getNoqaCode(psiComment: PsiComment, offset: Int): String? {
        val noqaCodes = extractNoqaCodes(psiComment) ?: return null
        if (noqaCodes.codes?.isNotEmpty() != true) return null
        if (noqaCodes.noqaStartOffset + "# noqa: ".length > offset || noqaCodes.noqaEndOffset < offset) return null
        val targetOffset = offset - psiComment.startOffset
        val startPart = psiComment.text.substring(0, targetOffset).takeLastWhile { it.isLetterOrDigit() }
        val endPart = psiComment.text.substring(targetOffset).takeWhile { it.isLetterOrDigit() }
        return startPart + endPart
    }}
