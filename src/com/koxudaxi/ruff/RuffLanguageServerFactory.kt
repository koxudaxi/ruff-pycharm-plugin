package com.koxudaxi.ruff
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider

class RuffLanguageServerFactory : LanguageServerFactory {
    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return RuffLanguageServer(project)
    }

    //If you need to provide client specific features
    override fun createLanguageClient(project: Project): LanguageClientImpl {
        return RuffLanguageClient(project)
    }
}