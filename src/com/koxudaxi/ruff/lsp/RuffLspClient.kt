package com.koxudaxi.ruff.lsp

interface RuffLspClient {
    fun start()
    fun stop()
    fun restart()
}