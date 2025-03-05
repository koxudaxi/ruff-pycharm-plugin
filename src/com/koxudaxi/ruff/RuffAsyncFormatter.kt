package com.koxudaxi.ruff

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiFile
import com.jetbrains.python.packaging.PyExecutionException
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

            private fun assertResult(currentText: String, text: String?): String? {
                return when {
                    text == null -> null
                    currentText == text -> null
                    else -> text
                }
            }

            override fun run() {
                if (cancelled) {
                    request.onTextReady(null)
                    return
                }
                runCatching {
                    val formattingContext: FormattingContext = request.context
                    val ioFile = request.ioFile ?: return@runCatching null
                    val sourceFile = formattingContext.containingFile.sourceFile
                    val currentText = ioFile.readText()

                    // Check if a formatting range is specified.
                    val formatRange = request.formattingRanges.firstOrNull()?.let { selectedRange ->
                        if (selectedRange.endOffset >= currentText.length) null
                        else selectedRange
                    }

                    if (formatRange != null) {
                        // When a range is specified, only run the format command.
                        val formatCommandArgs = generateCommandArgs(
                            sourceFile,
                            FORMAT_ARGS + formatRange.formatRangeArgs(currentText),
                            false
                        )
                            ?: return@runCatching null
                        val formatCommandStdout = runRuff(formatCommandArgs, currentText.toByteArray()) ?: return@runCatching null
                        return@runCatching assertResult(currentText, formatCommandStdout)
                    }

                    val fixCommandArgs = generateCommandArgs(sourceFile, formattingContext.project.FIX_ARGS, false)
                        ?: return@runCatching null
                    val fixCommandStdout = runRuff(fixCommandArgs, currentText.toByteArray()) ?: return@runCatching null
                    if (!RuffConfigService.getInstance(formattingContext.project).useRuffFormat) {
                        return@runCatching assertResult(currentText, fixCommandStdout)
                    }
                    val formatCommandArgs = generateCommandArgs(sourceFile, FORMAT_ARGS, false)
                        ?: return@runCatching null
                    if (cancelled) {
                        return@runCatching null
                    }
                    val formatCommandStdout = runRuff(formatCommandArgs, fixCommandStdout.toByteArray())
                    if (cancelled) {
                        return@runCatching null
                    }
                    assertResult(currentText, formatCommandStdout)
                }.onFailure { exception ->
                    when (exception) {
                        is ProcessCanceledException -> request.onTextReady(null)
                        is FileNotFoundException -> request.onTextReady(null)
                        is PyExecutionException -> request.onTextReady(null)
                        else -> request.onError("Ruff Error", exception.localizedMessage)
                    }
                }.onSuccess {
                    request.onTextReady(it)
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
