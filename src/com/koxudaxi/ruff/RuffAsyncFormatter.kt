package com.koxudaxi.ruff

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiFile
import java.io.FileNotFoundException


class RuffAsyncFormatter : AsyncDocumentFormattingService() {
    private val FEATURES: Set<FormattingService.Feature> = setOf(FormattingService.Feature.FORMAT_FRAGMENTS)

    override fun getFeatures(): Set<FormattingService.Feature> {
        return FEATURES
    }

    override fun canFormat(file: PsiFile): Boolean {
        val config = file.project.configService
        return config.runRuffOnReformatCode && file.isApplicableTo
    }

    override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask? {
        return object : FormattingTask {
            @Volatile
            private var cancelled = false

            private fun noUpdate() {
                request.onTextReady(null)
            }

            private fun updateText(currentText: String, text: String?) {
                when {
                    text == null -> noUpdate()
                    currentText == text -> noUpdate()
                    else -> request.onTextReady(text)
                }
            }

            override fun run() {
                if (cancelled) {
                    request.onTextReady(null)
                    return
                }
                runCatching {
                    val formattingContext: FormattingContext = request.context
                    val ioFile = request.ioFile
                    if (ioFile == null) {
                        request.onTextReady(null)
                        return@runCatching
                    }
                    val sourceFile = formattingContext.containingFile.sourceFile
                    val currentText = ioFile.readText()

                    // Check if a formatting range is specified.
                    val formatRange = request.formattingRanges.firstOrNull()?.let { selectedRange ->
                        if (selectedRange.endOffset >= currentText.length) null
                        else selectedRange
                    }

                    if (formatRange != null) {
                        // When a range is specified, only run the format command.
                        val formatCommandArgs = generateCommandArgs(sourceFile, FORMAT_ARGS + formatRange.formatRangeArgs(currentText), false)
                            ?: return@runCatching
                        val formatCommandStdout = runRuff(formatCommandArgs, currentText.toByteArray())
                        if (formatCommandStdout == null) {
                            request.onTextReady(null)
                            return@runCatching
                        }
                        updateText(currentText, formatCommandStdout)
                        return@runCatching
                    }

                    val fixCommandArgs = generateCommandArgs(sourceFile, formattingContext.project.FIX_ARGS, false)
                        ?: return@runCatching
                    val fixCommandStdout = runRuff(fixCommandArgs, currentText.toByteArray())
                    if (fixCommandStdout == null) {
                        request.onTextReady(null)
                        return@runCatching
                    }
                    if (!RuffConfigService.getInstance(formattingContext.project).useRuffFormat) {
                        updateText(currentText, fixCommandStdout)
                        return@runCatching
                    }
                    val formatCommandArgs = generateCommandArgs(sourceFile, FORMAT_ARGS, false)
                        ?: return@runCatching
                    if (cancelled) {
                        noUpdate()
                        return@runCatching
                    }
                    val formatCommandStdout = runRuff(formatCommandArgs, fixCommandStdout.toByteArray())
                    if (cancelled) {
                        noUpdate()
                        return@runCatching
                    }
                    updateText(currentText, formatCommandStdout)

                }.onFailure { exception ->
                    when (exception) {
                        is ProcessCanceledException -> { /* ignore */ }
                        is FileNotFoundException -> noUpdate()
                        else -> request.onError("Ruff Error", exception.localizedMessage)
                    }
                }
            }

            override fun cancel(): Boolean {
                cancelled = true
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
