package com.koxudaxi.ruff

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiFile
import java.util.*


class RuffAsyncFormatterFix : AsyncDocumentFormattingService() {
    private val FEATURES: MutableSet<FormattingService.Feature> = EnumSet.noneOf(
        FormattingService.Feature::class.java
    )

    override fun getFeatures(): MutableSet<FormattingService.Feature> {
        return FEATURES
    }

    override fun canFormat(file: PsiFile): Boolean {
        return RuffConfigService.getInstance(file.project).runRuffOnReformatCode && file.isApplicableTo
    }
    override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask? {
        val formattingContext: FormattingContext = request.context
        val ioFile = request.ioFile ?: return null
        return object : FormattingTask {
            private fun updateText(currentText: String, text: String?) {
                when {
                    text == null -> request.onTextReady(null)
                    currentText == text -> request.onTextReady(null)
                    else -> request.onTextReady(text)
                }
            }
            override fun run() {
                runCatching {
                    val sourceFile = formattingContext.containingFile.sourceFile
                    val fixCommandArgs =
                        generateCommandArgs(sourceFile, formattingContext.project.FIX_ARGS) ?: return@runCatching
                    val currentText = ioFile.readText()
                    val fixCommandStdout = runRuff(fixCommandArgs, currentText.toByteArray())
                    if (fixCommandStdout == null) {
                        request.onTextReady(null)
                        return@runCatching
                    }
                    updateText(currentText, fixCommandStdout)

                }.onFailure { exception ->
                    when (exception) {
                        is ProcessCanceledException -> { /* ignore */ }
                        else -> {
                            request.onError("Ruff Error", exception.localizedMessage)
                        }
                    }
                }
            }
            override fun cancel(): Boolean {
                return true
            }

            override fun isRunUnderProgress(): Boolean {
                return true
            }
        }
    }

    override fun getNotificationGroupId(): String {
        return "Ruff"
    }

    override fun getName(): String {
        return "Ruff Formatter Fix"
    }
}