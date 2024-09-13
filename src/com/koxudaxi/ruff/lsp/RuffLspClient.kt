package com.koxudaxi.ruff.lsp

interface RuffLspClient {
    fun start()
    fun stop()
    fun restart()
    fun getClientType(): ClientType
}


enum class ClientType {
    LSP4IJ, INTELLIJ
}
