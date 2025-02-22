package com.koxudaxi.ruff.lsp.lsp4ij

import com.koxudaxi.ruff.lsp.lsp4ij.features.*
import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.*
import com.redhat.devtools.lsp4ij.LanguageServerEnablementSupport
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.client.features.*
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider

@Suppress("UnstableApiUsage")
class RuffLanguageServerFactory : LanguageServerFactory, LanguageServerEnablementSupport {

    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return RuffLanguageServer(project)

    }

    //If you need to provide client specific features
    override fun createLanguageClient(project: Project): LanguageClientImpl {
        return RuffLanguageClient(project)
    }

    override fun isEnabled(project: Project): Boolean {
        val ruffConfigService = project.configService
        if (!ruffConfigService.enableLsp) return false
        if (!ruffConfigService.useRuffLsp && !ruffConfigService.useRuffServer) return false
        if (!ruffConfigService.useLsp4ij) return false
        if (!isInspectionEnabled(project)) return false
        val ruffCacheService = RuffCacheService.getInstance(project)
        if (ruffCacheService.getVersion() == null) return false
        if (ruffConfigService.useRuffServer && ruffCacheService.hasLsp() != true) return false
        if (ruffConfigService.useRuffLsp && getRuffExecutable(
                project,
                ruffConfigService,
                true,
                ruffCacheService
            ) == null
        ) return false
        return true
    }

    override fun setEnabled(p0: Boolean, project: Project) {
    }


    override fun createClientFeatures(): LSPClientFeatures {
        return LSPClientFeatures().setCodeActionFeature(RuffLSPCodeActionFeature())
            .setDiagnosticFeature(RuffLSPDiagnosticFeature())
            .setFormattingFeature(RuffLSPFormattingFeature())
            .setHoverFeature(RuffLSPHoverFeature())

    }
}