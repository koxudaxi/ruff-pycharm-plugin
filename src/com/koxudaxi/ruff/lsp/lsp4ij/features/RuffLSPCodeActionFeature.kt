package com.koxudaxi.ruff.lsp.lsp4ij.features

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.koxudaxi.ruff.RuffConfigService
import com.koxudaxi.ruff.configService
import com.koxudaxi.ruff.isApplicableTo
import com.koxudaxi.ruff.lsp.useCodeActionFeature
import com.redhat.devtools.lsp4ij.client.features.LSPCodeActionFeature
import com.redhat.devtools.lsp4ij.client.features.LSPHoverFeature

@Suppress("UnstableApiUsage")
class RuffLSPCodeActionFeature : LSPCodeActionFeature() {
    override fun isSupported(file: PsiFile): Boolean =
        file.isApplicableTo

    override fun isEnabled(file: PsiFile): Boolean =
        project.configService.codeActionFeature
}