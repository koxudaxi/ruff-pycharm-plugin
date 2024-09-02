package com.koxudaxi.ruff.lsp.intellij

import RuffLspServerSupportProvider
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerManager
import com.koxudaxi.ruff.intellijLspClientSupported
import com.koxudaxi.ruff.lsp.RuffLspClient

class RuffIntellijLspClient(val project: Project) : RuffLspClient {
    @Suppress("UnstableApiUsage")
    private val lspServerManager = if (intellijLspClientSupported) LspServerManager.getInstance(project) else null

    override fun start() {
        restart()
    }

    override fun stop() {
        if (lspServerManager == null) return
        @Suppress("UnstableApiUsage")
        lspServerManager.stopServers(RuffLspServerSupportProvider::class.java)
    }

    override fun restart() {
        if (lspServerManager == null) return
        @Suppress("UnstableApiUsage")
        lspServerManager.stopAndRestartIfNeeded(RuffLspServerSupportProvider::class.java)
    }
}