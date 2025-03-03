package com.koxudaxi.ruff.lsp.lsp4ij

import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.lsp.ClientType
import com.koxudaxi.ruff.lsp.RuffLspClient
import com.koxudaxi.ruff.lsp4ijSupported
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.redhat.devtools.lsp4ij.ServerStatus

class RuffLsp4IntellijClient(project: Project) : RuffLspClient {
    private val languageServerManager = if(lsp4ijSupported) LanguageServerManager.getInstance(project) else null
    private val isRunning: Boolean
        get() {
            if (languageServerManager == null) return false
            val status = languageServerManager.getServerStatus(LANGUAGE_SERVER_ID)
            return status == ServerStatus.started || status == ServerStatus.starting
        }

    override fun getLanguageServerManager(): LanguageServerManager? {
        return languageServerManager
    }

    override fun start() {
        if (languageServerManager == null) return
        if (!isRunning) {
            languageServerManager.start(LANGUAGE_SERVER_ID)
        }
    }


    override fun stop() {
        if (languageServerManager == null) return
        if (isRunning) {
            languageServerManager.stop(LANGUAGE_SERVER_ID)
        }
    }

    override fun restart() {
        if (languageServerManager == null) return
        if (isRunning) {
            languageServerManager.stop(LANGUAGE_SERVER_ID)
        }
        languageServerManager.start(LANGUAGE_SERVER_ID)
    }

    override fun getClientType(): ClientType {
        return ClientType.LSP4IJ
    }

    companion object {
        const val LANGUAGE_SERVER_ID = "ruffLanguageServer"

    }
}