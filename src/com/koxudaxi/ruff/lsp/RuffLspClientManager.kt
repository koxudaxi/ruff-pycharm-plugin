package com.koxudaxi.ruff.lsp

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project


@Service(Service.Level.PROJECT)
class RuffLspClientManager {
    private fun getLspClient(): List<RuffLspClient> {
        return RuffLspClient.EP_NAME.extensionList
    }
    private val lspClient: List<RuffLspClient> = getLspClient()
    companion object {
        fun getInstance(project: Project): RuffLspClientManager {
            return project.getService(RuffLspClientManager::class.java)
        }
    }
}