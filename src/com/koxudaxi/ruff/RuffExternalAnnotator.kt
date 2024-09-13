package com.koxudaxi.ruff

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.InspectionProfile
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.impl.PyFileImpl


class RuffExternalAnnotator :
    ExternalAnnotator<RuffExternalAnnotator.RuffExternalAnnotatorInfo, RuffExternalAnnotator.RuffExternalAnnotatorResult>() {
    private val highlightSeverityLevels = mapOf<HighlightDisplayLevel, HighlightSeverity>(
        HighlightDisplayLevel.ERROR to HighlightSeverity.ERROR,
        HighlightDisplayLevel.WARNING to HighlightSeverity.WARNING,
        HighlightDisplayLevel.WEAK_WARNING to HighlightSeverity.WEAK_WARNING,
    )
    private val problemHighlightTypeLevels = mapOf(
        HighlightDisplayLevel.ERROR to ProblemHighlightType.ERROR,
        HighlightDisplayLevel.WARNING to ProblemHighlightType.WARNING,
        HighlightDisplayLevel.WEAK_WARNING to ProblemHighlightType.WEAK_WARNING,
    )

    override fun getPairedBatchInspectionShortName(): String = RuffInspection.INSPECTION_SHORT_NAME

    override fun collectInformation(file: PsiFile): RuffExternalAnnotatorInfo? {
        if (file !is PyFile) return null
        val project = file.project
        val config = RuffConfigService.getInstance(project)
        if (lspSupported && config.enableLsp) {
            return null
            // May be too strict check
//            if (config.useRuffLsp && config.ruffLspExecutablePath is String) return null
//            if (config.useRuffServer && RuffCacheService.hasLsp(project) == true) return null
        }
        if (!file.isApplicableTo) return null
        val profile: InspectionProfile = InspectionProjectProfileManager.getInstance(project).currentProfile
        val key = HighlightDisplayKey.find(RuffInspection.INSPECTION_SHORT_NAME) ?: return null
        if (!profile.isToolEnabled(key, file)) return null
        if (file is PyFileImpl && !file.isAcceptedFor(RuffInspection::class.java)) return null
        val level = profile.getErrorLevel(key, file)
        val highlightDisplayLevel = highlightSeverityLevels[level] ?: return null
        val problemHighlightType = problemHighlightTypeLevels[level] ?: return null
        val showRuleCode = config.showRuleCode
        val noFixArgs = project.NO_FIX_ARGS ?: return null
        val commandArgs = generateCommandArgs(file.sourceFile, noFixArgs) ?: return null
        return RuffExternalAnnotatorInfo(showRuleCode, highlightDisplayLevel, problemHighlightType, commandArgs)
    }

    override fun doAnnotate(info: RuffExternalAnnotatorInfo?): RuffExternalAnnotatorResult? {
        if (info == null) return null
        val response = runRuff(info.commandArgs) ?: return null
        val result = parseJsonResponse(response)
        return RuffExternalAnnotatorResult(
            info.showRuleCode,
            info.highlightDisplayLevel,
            info.problemHighlightType,
            result
        )
    }

    override fun apply(file: PsiFile, annotationResult: RuffExternalAnnotatorResult?, holder: AnnotationHolder) {
        if (annotationResult == null) return
        val project = file.project
        val showRuleCode = annotationResult.showRuleCode
        val result = annotationResult.result
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        val inspectionManager = InspectionManager.getInstance(file.project)

        result.forEach {
            val range = document.getStartEndRange(it.location, it.endLocation, -1)
            if (range.startOffset == 0 && range.endOffset == 0) return@forEach
            val psiElement = getPyElement(file, range) ?: return@forEach
            val builder = holder.newAnnotation(
                annotationResult.highlightDisplayLevel,
                if (showRuleCode) "${it.code} ${it.message}" else it.message
            ).needsUpdateOnTyping()
            if (it.fix != null) {
                RuffQuickFix.create(it.fix, document)?.let { quickFix ->
                    val problemDescriptor = inspectionManager.createProblemDescriptor(
                        psiElement,
                        it.message,
                        quickFix,
                        annotationResult.problemHighlightType,
                        true
                    )
                    builder.newLocalQuickFix(quickFix, problemDescriptor).registerFix()
                }
            }

            RuffSuppressQuickFix.create(it, document).let { quickFix ->
                val problemDescriptor = inspectionManager.createProblemDescriptor(
                    psiElement,
                    quickFix.familyName,
                    quickFix,
                    annotationResult.problemHighlightType,
                    true
                )
                builder.newLocalQuickFix(quickFix, problemDescriptor).registerFix()
            }
            builder.range(range)
            if (range.startOffset == range.endOffset && (range.endOffset == file.textLength || file.text.substring(range.startOffset, range.endOffset + 1) == "\n")) {
                builder.afterEndOfLine()
            }
            builder.create()
        }

    }

    private fun isForFile(document: Document, result: Result, trimOriginalLength: Int, range: TextRange): Boolean {
        if (result.location.row == 1 && result.location.column == 1) {
            val trimResultLength = range.substring(document.text).trimEnd().length
            if (trimResultLength == trimOriginalLength) {
                return true
            }
        }
        return false
    }

    private fun getPyElement(psiFile: PsiFile, range: TextRange): PsiElement? {
        val psiElement = PsiTreeUtil.findElementOfClassAtRange(
        psiFile,
        range.startOffset,
        range.endOffset,
        PsiElement::class.java
        )

        if (psiElement != null) return psiElement
        if (range.startOffset == range.endOffset && range.endOffset == psiFile.textLength) {
            return psiFile.findElementAt(range.endOffset - 1)
        }
        return psiFile.findElementAt(range.startOffset)
    }

    data class RuffExternalAnnotatorInfo(
        val showRuleCode: Boolean,
        val highlightDisplayLevel: HighlightSeverity,
        val problemHighlightType: ProblemHighlightType,
        val commandArgs: CommandArgs
    )

    data class RuffExternalAnnotatorResult(
        val showRuleCode: Boolean,
        val highlightDisplayLevel: HighlightSeverity,
        val problemHighlightType: ProblemHighlightType,
        val result: List<Result>
    )
}