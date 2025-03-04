package com.koxudaxi.ruff

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiFile


class RuffAsyncFormatter : AsyncDocumentFormattingService() {
    private val FEATURES: Set<FormattingService.Feature> = setOf(FormattingService.Feature.AD_HOC_FORMATTING)

    override fun getFeatures(): Set<FormattingService.Feature> {
        return FEATURES
    }

    override fun canFormat(file: PsiFile): Boolean {
        val config = file.project.configService
        return config.runRuffOnReformatCode && file.isApplicableTo
    }

    override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask? {
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
                    val formattingContext: FormattingContext = request.context
                    val ioFile = request.ioFile
                    if (ioFile == null) {
                        request.onTextReady(null)
                        return@runCatching
                    }
                    val sourceFile = formattingContext.containingFile.sourceFile
                    val fixCommandArgs =
                        generateCommandArgs(sourceFile, formattingContext.project.FIX_ARGS) ?: return@runCatching
                    val currentText = ioFile.readText()
                    val fixCommandStdout = runRuff(fixCommandArgs, currentText.toByteArray())
                    if (fixCommandStdout == null) {
                        request.onTextReady(null)
                        return@runCatching
                    }
                    if (!RuffConfigService.getInstance(formattingContext.project).useRuffFormat) {
                        updateText(currentText, fixCommandStdout)
                        return@runCatching
                    }
                    val formatCommandArgs = generateCommandArgs(sourceFile, FORMAT_ARGS)
                    if (formatCommandArgs == null) {
                        updateText(currentText, fixCommandStdout)
                        return@runCatching
                    }
                    val formatCommandStdout = runRuff(formatCommandArgs, fixCommandStdout.toByteArray())
                    updateText(currentText, formatCommandStdout)

                }.onFailure { exception ->
                    when (exception) {
                        is ProcessCanceledException -> { /* ignore */
                        }

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
        return "Ruff Formatter"
    }
}