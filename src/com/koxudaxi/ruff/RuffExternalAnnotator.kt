package com.koxudaxi.ruff

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyFile


class RuffExternalAnnotator :
    ExternalAnnotator<RuffExternalAnnotator.RuffExternalAnnotatorInfo, RuffExternalAnnotator.RuffExternalAnnotatorResult>() {
    private val argsBase =
        listOf("--exit-zero", "--no-cache", "--no-fix", "--format", "json")

    override fun collectInformation(file: PsiFile): RuffExternalAnnotatorInfo? {
        if (file !is PyFile) return null
        if (!file.isApplicableTo) return null
        val showRuleCode = RuffConfigService.getInstance(file.project).showRuleCode
        val commandArgs =  generateCommandArgs(file, argsBase)
        return RuffExternalAnnotatorInfo(showRuleCode, commandArgs)
    }

    override fun doAnnotate(info: RuffExternalAnnotatorInfo): RuffExternalAnnotatorResult? {
        val showRuleCode = info.showRuleCode
        val response = runRuff(info.commandArgs) ?: return null
        val result = parseJsonResponse(response)
        return RuffExternalAnnotatorResult(showRuleCode, result)
    }

    override fun apply(file: PsiFile, annotationResult: RuffExternalAnnotatorResult?, holder: AnnotationHolder) {
        if (annotationResult == null) return
        val project = file.project
        val showRuleCode = annotationResult.showRuleCode
        val result = annotationResult.result
        val trimOriginalLength = file.text.trimEnd().length
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        val t = document.text
        val tt = file.text
        result.forEach {
            val psiElement = getPyElement(it, file, document) ?: return@forEach
            val builder = holder.newAnnotation(
                HighlightSeverity.WARNING,
                if (showRuleCode) "${it.code} ${it.message}" else it.message
            )
            if (it.fix != null) {
                RuffQuickFix.create(it.fix, document)?.let { quickFix ->
                    val problemDescriptor = InspectionManager.getInstance(file.project)
                        .createProblemDescriptor(psiElement, it.message, quickFix, ProblemHighlightType.WARNING, true)
                    builder.needsUpdateOnTyping().newLocalQuickFix(quickFix, problemDescriptor).registerFix() }
            }
            if (isForFile(document, it, trimOriginalLength)) {
                builder.fileLevel()
            } else {
                builder.range(psiElement)
            }
            builder.create()
        }

    }

    private fun isForFile(document: Document, result: Result, trimOriginalLength: Int): Boolean {
        if (result.location.row == 1 && result.location.column == 1) {
            val range = document.getStartEndRange(result.location, result.endLocation, -   1)
            val trimResultLength = range.substring(document.text).trimEnd().length
            if (trimResultLength == trimOriginalLength) {
                return true
            }
        }
        return false
    }

    private fun getPyElement(result: Result, psiFile: PsiFile, document: Document): PsiElement? {
        document.getStartEndRange(result.location, result.endLocation, -1).let {
            return PsiTreeUtil.findElementOfClassAtRange(
                psiFile,
                it.startOffset,
                it.endOffset,
                PsiElement::class.java
            )
        }
    }

    data class RuffExternalAnnotatorInfo(
        val showRuleCode: Boolean,
        val commandArgs: CommandArgs
    )

    data class RuffExternalAnnotatorResult(
        val showRuleCode: Boolean,
        val result: List<Result>
    )
}