package com.koxudaxi.ruff.lsp.lsp4ij.features

import com.intellij.psi.PsiFile
import com.koxudaxi.ruff.isApplicableTo
import com.koxudaxi.ruff.lsp.useFormattingFeature
import com.redhat.devtools.lsp4ij.client.features.LSPFormattingFeature

@Suppress("UnstableApiUsage")
class RuffLSPFormattingFeature : LSPFormattingFeature() {
    override fun isSupported(file: PsiFile): Boolean =
        file.isApplicableTo

    override fun isEnabled(file: PsiFile): Boolean =
        project.useFormattingFeature
}