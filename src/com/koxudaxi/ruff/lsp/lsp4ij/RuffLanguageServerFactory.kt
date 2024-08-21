package com.koxudaxi.ruff.lsp.lsp4ij
import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.RuffCacheService
import com.koxudaxi.ruff.RuffConfigService
import com.koxudaxi.ruff.isInspectionEnabled
import com.redhat.devtools.lsp4ij.LanguageServerEnablementSupport
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider

class RuffLanguageServerFactory : LanguageServerFactory, LanguageServerEnablementSupport {

    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return RuffLanguageServer(project)
    }

    //If you need to provide client specific features
    override fun createLanguageClient(project: Project): LanguageClientImpl {
        return RuffLanguageClient(project)
    }

    override fun isEnabled(project: Project): Boolean {
        val ruffConfigService = RuffConfigService.getInstance(project)
        if (!ruffConfigService.useRuffLsp && !ruffConfigService.useRuffServer) return false
        if (!ruffConfigService.useLsp4ij) return false
        if (!isInspectionEnabled(project)) return false

        val ruffCacheService = RuffCacheService.getInstance(project)
        return ruffCacheService.getVersion() != null
    }

    override fun setEnabled(p0: Boolean, project: Project) {
    }
}