package com.koxudaxi.ruff.lsp.lsp4ij

import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.lsp.RuffLspClient

class RuffLsp4IntellijClient(val project: Project): RuffLspClient {
    private val lspServer get() = RuffLanguageServerFactory().createConnectionProvider(project)
    override fun start() {
        lspServer.start()
    }

    override fun stop() {
        if (lspServer.isAlive) {
            lspServer.stop()
        }
    }

    override fun restart() {
        if (lspServer.isAlive) {
            lspServer.stop()
        }
        lspServer.start()
    }
    companion object {
        const val RUFF_LSP_CLIENT_ID = "ruff-lsp4-intellij-client"
    }
}