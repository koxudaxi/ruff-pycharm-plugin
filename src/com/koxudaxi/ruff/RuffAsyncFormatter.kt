package com.koxudaxi.ruff

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.psi.PsiFile
import java.nio.charset.StandardCharsets
import java.util.*


class RuffAsyncFormatter : AsyncDocumentFormattingService() {
    private val FEATURES: MutableSet<FormattingService.Feature> = EnumSet.noneOf(
            FormattingService.Feature::class.java
    )

    override fun getFeatures(): MutableSet<FormattingService.Feature> {
        return FEATURES
    }

    override fun canFormat(file: PsiFile): Boolean {
        return file.isApplicableTo
    }

    override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask? {
        val formattingContext: FormattingContext = request.context
        val ioFile = request.ioFile ?: return null
        val sourceFile = formattingContext.containingFile.sourceFile
        val commandArgs = generateCommandArgs(sourceFile, FORMAT_ARGS) ?: return null
        try {
            val commandLine = getGeneralCommandLine(commandArgs.executable, commandArgs.project, *commandArgs.args.toTypedArray())
                    ?: return null
            val handler = OSProcessHandler(commandLine.withCharset(StandardCharsets.UTF_8))
            with(handler) {
                processInput.write(ioFile.readText().toByteArray())
                processInput.close()
            }
            return object : FormattingTask {
                override fun run() {
                    handler.addProcessListener(object : CapturingProcessAdapter() {
                        override fun processTerminated(event: ProcessEvent) {
                            val exitCode = event.exitCode
                            if (exitCode == 0) {
                                request.onTextReady(output.stdout)
                            } else {
                                request.onError("Ruff Error", output.stderr)
                            }
                        }
                    })
                    handler.startNotify()
                }

                override fun cancel(): Boolean {
                    handler.destroyProcess()
                    return true
                }

                override fun isRunUnderProgress(): Boolean {
                    return true
                }
            }
        } catch (e: ExecutionException) {
            e.message?.let { request.onError("Ruff Error", it) }
            return null
        }
    }

    override fun getNotificationGroupId(): String {
        return "Ruff"
    }

    override fun getName(): String {
        return "Ruff formatter"
    }
}