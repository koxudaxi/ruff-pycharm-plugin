package com.koxudaxi.ruff.lsp.intellij.supports

import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.customization.LspCodeActionsSupport
import com.intellij.platform.lsp.api.customization.LspIntentionAction
import org.eclipse.lsp4j.CodeAction

@Suppress("UnstableApiUsage")
class RuffLspCodeActionsSupport(val project: Project): LspCodeActionsSupport() {
    override val intentionActionsSupport: Boolean = true
    override val quickFixesSupport: Boolean = true
    override fun createIntentionAction(lspServer: LspServer, codeAction: CodeAction): LspIntentionAction {
        return LspIntentionAction(lspServer, codeAction)
    }
    override fun createQuickFix(lspServer: LspServer, codeAction: CodeAction): LspIntentionAction {
        return LspIntentionAction(lspServer, codeAction)
    }

}