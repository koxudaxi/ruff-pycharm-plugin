package com.koxudaxi.ruff.lsp.intellij.supports

import com.intellij.platform.lsp.api.customization.LspCodeActionsSupport

class RuffLspCodeActionsSupport: LspCodeActionsSupport() {
    override val intentionActionsSupport: Boolean
        // TODO: Implement this method
        get() = true
    override val quickFixesSupport: Boolean
        // TODO: Implement this method
        get() = true

}