package com.koxudaxi.ruff.lsp.lsp4ij.features

import com.intellij.psi.PsiFile
import com.koxudaxi.ruff.isApplicableTo
import com.koxudaxi.ruff.lsp.useHoverFeature
import com.redhat.devtools.lsp4ij.client.features.LSPHoverFeature

@Suppress("UnstableApiUsage")
class RuffLSPHoverFeature : LSPHoverFeature() {
    override fun isSupported(file: PsiFile): Boolean =
        file.isApplicableTo

    override fun isEnabled(file: PsiFile): Boolean =
        project.useHoverFeature
}