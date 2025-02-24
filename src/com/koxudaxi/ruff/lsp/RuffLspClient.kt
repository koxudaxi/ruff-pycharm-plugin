package com.koxudaxi.ruff.lsp

import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.configService

interface RuffLspClient {
    fun start()
    fun stop()
    fun restart()
    fun getClientType(): ClientType
}


enum class ClientType {
    LSP4IJ, INTELLIJ
}

val Project.useCodeActionFeature: Boolean
    get() =  this.configService.codeActionFeature
val Project.useFormattingFeature: Boolean
    get() =  this.configService.formattingFeature
val Project.useHoverFeature: Boolean
    get() =  this.configService.hoverFeature
val Project.useDiagnosticFeature: Boolean
    get() =  this.configService.diagnosticFeature
val Project.runRuffOnReformatCode: Boolean
    get() =  this.configService.runRuffOnReformatCode