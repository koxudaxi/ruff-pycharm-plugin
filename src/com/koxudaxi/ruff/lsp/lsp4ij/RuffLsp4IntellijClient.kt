package com.koxudaxi.ruff.lsp.lsp4ij

import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.lsp.RuffLspClient
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.redhat.devtools.lsp4ij.ServerStatus

class RuffLsp4IntellijClient(project: Project) : RuffLspClient {
    private val languageServerManager: LanguageServerManager = LanguageServerManager.getInstance(project)
    private val isRunning: Boolean
        get() {
            val status = languageServerManager.getServerStatus(LANGUAGE_SERVER_ID)
            return status == ServerStatus.started || status == ServerStatus.starting
        }

    override fun start() {
        if (!isRunning) {
            languageServerManager.start(LANGUAGE_SERVER_ID)
        }
    }


    override fun stop() {
        if (isRunning) {
            languageServerManager.stop(LANGUAGE_SERVER_ID)
        }
    }

    override fun restart() {
        if (isRunning) {
            languageServerManager.stop(LANGUAGE_SERVER_ID)
        }
        languageServerManager.start(LANGUAGE_SERVER_ID)
    }
    companion object {
        const val LANGUAGE_SERVER_ID = "ruffLanguageServer"

    }
}