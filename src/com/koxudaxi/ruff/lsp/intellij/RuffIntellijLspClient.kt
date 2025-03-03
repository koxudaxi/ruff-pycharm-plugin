package com.koxudaxi.ruff.lsp.intellij

import RuffLspServerSupportProvider
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerManager
import com.koxudaxi.ruff.RuffLoggingService
import com.koxudaxi.ruff.intellijLspClientSupported
import com.koxudaxi.ruff.lsp.ClientType
import com.koxudaxi.ruff.lsp.RuffLspClient

class RuffIntellijLspClient(val project: Project) : RuffLspClient {
    @Suppress("UnstableApiUsage")
    private val lspServerManager = if (intellijLspClientSupported) LspServerManager.getInstance(project) else null
    fun getLspServerManager(): LspServerManager? {
        return lspServerManager
    }
    override fun start() {
        RuffLoggingService.log(project, "Starting IntelliJ LSP client...")
        restart()
    }

    override fun stop() {
        if (lspServerManager == null) return
        RuffLoggingService.log(project, "Stopping IntelliJ LSP client...")
        @Suppress("UnstableApiUsage")
        lspServerManager.stopServers(RuffLspServerSupportProvider::class.java)
    }

    override fun restart() {
        if (lspServerManager == null) return
        RuffLoggingService.log(project, "Restarting IntelliJ LSP client...")
        @Suppress("UnstableApiUsage")
        lspServerManager.stopAndRestartIfNeeded(RuffLspServerSupportProvider::class.java)
    }

    override fun getClientType(): ClientType {
        return ClientType.INTELLIJ
    }

    override fun getLanguageServerManager(): Any? {
        TODO("Not yet implemented")
    }
}