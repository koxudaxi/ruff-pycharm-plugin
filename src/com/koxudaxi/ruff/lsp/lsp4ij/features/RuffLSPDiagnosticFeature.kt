package com.koxudaxi.ruff.lsp.lsp4ij.features

import com.intellij.psi.PsiFile
import com.koxudaxi.ruff.isApplicableTo
import com.koxudaxi.ruff.lsp.useDiagnosticFeature
import com.redhat.devtools.lsp4ij.client.features.LSPDiagnosticFeature

@Suppress("UnstableApiUsage")
class RuffLSPDiagnosticFeature : LSPDiagnosticFeature() {
    override fun isSupported(file: PsiFile): Boolean =
        file.isApplicableTo

    override fun isEnabled(file: PsiFile): Boolean =
    project.useDiagnosticFeature
}