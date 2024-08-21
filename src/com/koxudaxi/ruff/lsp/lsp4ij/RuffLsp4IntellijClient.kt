package com.koxudaxi.ruff.lsp.lsp4ij

import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.lsp.RuffLspClient
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider

class RuffLsp4IntellijClient(val project: Project): RuffLspClient {
    private var provider: StreamConnectionProvider? = null
    override fun start() {
        provider?.let {
            if (!it.isAlive) {
                it.start()
            }
        } ?: run {
            val newProvider = RuffLanguageServerFactory().createConnectionProvider(project)
            newProvider.start()
            provider = newProvider
        }
    }

    override fun stop() {
        provider?.let {
            if (it.isAlive) {
                it.stop()
            }
        }
    }

    override fun restart() {
        provider?.let {
            if (it.isAlive) {
                it.stop()
            }
        }
        start()
    }
}