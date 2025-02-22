package com.koxudaxi.ruff.lsp.intellij.supports

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.customization.LspDiagnosticsSupport
import org.eclipse.lsp4j.Diagnostic

@Suppress("UnstableApiUsage")
class RuffLspDiagnosticsSupport(val project: Project) : LspDiagnosticsSupport() {
    // TODO: Implement these methods
}