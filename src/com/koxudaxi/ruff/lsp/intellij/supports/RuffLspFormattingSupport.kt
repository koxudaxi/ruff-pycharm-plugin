package com.koxudaxi.ruff.lsp.intellij.supports

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.customization.LspFormattingSupport

@Suppress("UnstableApiUsage")
class RuffLspFormattingSupport: LspFormattingSupport() {
    override fun shouldFormatThisFileExclusivelyByServer(
        file: VirtualFile,
        ideCanFormatThisFileItself: Boolean,
        serverExplicitlyWantsToFormatThisFile: Boolean
    ): Boolean {
    // TODO: Implement this method
        return true
    }
}