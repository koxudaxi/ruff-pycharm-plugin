package com.koxudaxi.ruff.lsp.intellij.supports

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.customization.LspDiagnosticsSupport
import com.koxudaxi.ruff.RuffLoggingService
import com.koxudaxi.ruff.buildWslFileUri
import org.eclipse.lsp4j.Diagnostic


@Suppress("UnstableApiUsage")
class RuffLspDiagnosticsSupport(private val project: Project) : LspDiagnosticsSupport() {
    override fun shouldAskServerForDiagnostics(file: VirtualFile): Boolean {
        val shouldAsk = super.shouldAskServerForDiagnostics(file)
        if (shouldLogWindowsWslDiagnostics(file)) {
            RuffLoggingService.log(
                project,
                "LSP pull diagnostics enabled: file=${file.path}, uri=${buildWslFileUri(file.path) ?: file.path}"
            )
        }
        return shouldAsk
    }

    override fun createAnnotation(
        holder: AnnotationHolder,
        diagnostic: Diagnostic,
        textRange: TextRange,
        quickFixes: List<IntentionAction>
    ) {
        if (shouldLogWindowsWslDiagnostics(diagnostic)) {
            RuffLoggingService.log(
                project,
                "LSP diagnostic annotation: code=${diagnostic.code?.left ?: diagnostic.code?.right}, message=${diagnostic.message}, range=${textRange.startOffset}-${textRange.endOffset}"
            )
        }
        super.createAnnotation(holder, diagnostic, textRange, quickFixes)
    }

    private fun shouldLogWindowsWslDiagnostics(file: VirtualFile): Boolean =
        SystemInfo.isWindows && buildWslFileUri(file.path) != null

    private fun shouldLogWindowsWslDiagnostics(diagnostic: Diagnostic): Boolean =
        SystemInfo.isWindows && diagnostic.source == "Ruff"
}
