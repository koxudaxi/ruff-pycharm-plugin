package com.koxudaxi.ruff.lsp

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.lsp.intellij.RuffIntellijLspClient
import com.koxudaxi.ruff.lsp.lsp4ij.RuffLsp4IntellijClient


@Service(Service.Level.PROJECT)
class RuffLspClientManager(private val project: Project) {
    private val ruffLsp4IntellijClient =  RuffLsp4IntellijClient(project)
    private val ruffIntellijLspClient = RuffIntellijLspClient(project)

    fun startRuffLsp4IntellijClient() {
        ruffLsp4IntellijClient.start()
    }
    fun stopRuffLsp4IntellijClient() {
        ruffLsp4IntellijClient.stop()
    }
    fun startRuffIntellijLspClient() {
        ruffIntellijLspClient.start()
    }
    fun stopRuffIntellijLspClient() {
        ruffIntellijLspClient.stop()
    }

    fun stopAll() {
        stopRuffIntellijLspClient()
        stopRuffLsp4IntellijClient()
    }

    companion object {
        fun getInstance(project: Project): RuffLspClientManager {
            return project.getService(RuffLspClientManager::class.java)
        }
    }


}