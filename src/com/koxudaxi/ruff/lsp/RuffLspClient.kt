package com.koxudaxi.ruff.lsp

import com.intellij.openapi.extensions.ExtensionPointName

interface RuffLspClient {
    companion object {
        val EP_NAME = ExtensionPointName.create<RuffLspClient>("com.koxudaxi.ruff.lsp.client")
    }
    fun getRuffLspClientId(): String
    fun start()
    fun stop()
    fun restart()
}