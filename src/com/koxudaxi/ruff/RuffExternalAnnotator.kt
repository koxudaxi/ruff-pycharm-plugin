package com.koxudaxi.ruff

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyUtil


class RuffExternalAnnotator :
    ExternalAnnotator<RuffExternalAnnotator.RuffExternalAnnotatorInfo, RuffExternalAnnotator.RuffExternalAnnotatorResult>() {
    private val argsBase =
        listOf("--exit-zero", "--no-cache", "--no-fix", "--format", "json")

    override fun collectInformation(file: PsiFile): RuffExternalAnnotatorInfo? {
        if (file !is PyFile) return null
        if (!file.isApplicableTo) return null
        val showRuleCode = RuffConfigService.getInstance(file.project).showRuleCode
        return RuffExternalAnnotatorInfo(file, showRuleCode)
    }

    override fun doAnnotate(info: RuffExternalAnnotatorInfo): RuffExternalAnnotatorResult? {
        val pyFile = info.pyFile
        val showRuleCode = info.showRuleCode
        val response = executeOnPooledThread(null) {
            runRuff(pyFile, argsBase)
        } ?: return null
        return RuffExternalAnnotatorResult(pyFile, showRuleCode, parseJsonResponse(response))
    }

    override fun apply(file: PsiFile, annotationResult: RuffExternalAnnotatorResult?, holder: AnnotationHolder) {
        if (annotationResult == null) return
        val pyFile = annotationResult.pyFile
        val project = pyFile.project
        val showRuleCode = annotationResult.showRuleCode
        val document = PsiDocumentManager.getInstance(project).getDocument(pyFile) ?: return

        annotationResult.result.forEach {
            val psiElement = getPyElement(it, pyFile, document) ?: return@forEach
            val builder = holder.newAnnotation(
                HighlightSeverity.WARNING,
                if (showRuleCode) "${it.code} ${it.message}" else it.message
            ).range(psiElement)
            if (it.fix != null) {
                RuffIntentionFix.create(it.fix, document)?.let { builder.withFix(it) }
            }
            builder.create()
        }

    }

    class RuffIntentionFix(private val edits: List<Edit>, private val message: String?) : IntentionAction {
        override fun startInWriteAction(): Boolean = true
        override fun getText(): String =
            when (message) {
                is String -> message
                else -> DEFAULT_FIX_MESSAGE
            }

        private val DEFAULT_FIX_MESSAGE = "Quick fix with ruff"
        override fun getFamilyName(): String =
            when (message) {
                is String -> message
                else -> DEFAULT_FIX_MESSAGE
            }

        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
            if (file == null) return
            PyUtil.updateDocumentUnblockedAndCommitted(
                file
            ) { document: Document ->
                edits.forEach { edit ->
                    document.replaceString(
                        edit.range.startOffset,
                        edit.range.endOffset,
                        edit.content
                    )
                }

            }
        }

        data class Edit(
            val content: String,
            val range: TextRange,
        )

        companion object {
            fun create(fix: Fix, document: Document): RuffIntentionFix? {
                return when {
                    fix.edits != null -> {
                        var offset = 0
                        fix.edits.map {
                            val range = document.getStartEndRange(it.location, it.endLocation, offset)
                            offset = offset - range.length + it.content.length
                            Edit(it.content, range)
                        }
                    }

                    fix.location != null && fix.endLocation != null && fix.content != null -> listOf(
                        Edit(
                            fix.content,
                            document.getStartEndRange(fix.location, fix.endLocation, 0)
                        )
                    )

                    else -> null
                }?.let {
                    RuffIntentionFix(it, fix.message)
                }
            }
        }
    }

    private fun getPyElement(result: Result, pyFile: PyFile, document: Document): PsiElement? {
        document.getStartEndRange(result.location, result.endLocation, -1).let {
            return PsiTreeUtil.findElementOfClassAtRange(
                pyFile,
                it.startOffset,
                it.endOffset,
                PsiElement::class.java
            )
        }
    }

    data class RuffExternalAnnotatorInfo(
        val pyFile: PyFile,
        val showRuleCode: Boolean
    )

    data class RuffExternalAnnotatorResult(
        val pyFile: PyFile,
        val showRuleCode: Boolean,
        val result: List<Result>
    )
}