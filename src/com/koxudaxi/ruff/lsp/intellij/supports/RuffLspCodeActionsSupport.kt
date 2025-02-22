package com.koxudaxi.ruff.lsp.intellij.supports

import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.customization.LspCodeActionsSupport

@Suppress("UnstableApiUsage")
class RuffLspCodeActionsSupport(val project: Project): LspCodeActionsSupport() {
    override val intentionActionsSupport: Boolean
        get() = true
    override val quickFixesSupport: Boolean
        get() = true

}