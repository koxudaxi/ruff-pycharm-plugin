package com.koxudaxi.ruff.lsp.intellij.supports

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.customization.LspFormattingSupport
import com.koxudaxi.ruff.isApplicableTo
import com.koxudaxi.ruff.lsp.runRuffOnReformatCode

@Suppress("UnstableApiUsage")
class RuffLspFormattingSupport(val project: Project): LspFormattingSupport() {
    override fun shouldFormatThisFileExclusivelyByServer(
        file: VirtualFile,
        ideCanFormatThisFileItself: Boolean,
        serverExplicitlyWantsToFormatThisFile: Boolean
    ): Boolean = project.runRuffOnReformatCode && file.isApplicableTo
}