package com.koxudaxi.ruff.lsp.lsp4ij.features

import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.client.features.LSPHoverFeature

@Suppress("UnstableApiUsage")
class RuffLSPHoverFeature : LSPHoverFeature() {
    override fun isSupported(file: PsiFile): Boolean {
        // TODO: Implement this method
        return true
    }
    override fun isEnabled(file: PsiFile): Boolean {
        // TODO: Implement this method
        return true
    }
}