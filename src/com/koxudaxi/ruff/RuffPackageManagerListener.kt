package com.koxudaxi.ruff

import RuffLspServerSupportProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.lsp.api.LspServerManager
import com.jetbrains.python.packaging.PyPackageManager

class RuffPackageManagerListener(project: Project) : PyPackageManager.Listener {
    private val ruffConfigService = RuffConfigService.getInstance(project)
    private val ruffCacheService = RuffCacheService.getInstance(project)
    private val ruffMacroService = RuffMacroService.getInstance(project)
    @Suppress("UnstableApiUsage")
    private val lspServerManager = if (lspIsSupported) LspServerManager.getInstance(project) else null
    override fun packagesRefreshed(sdk: Sdk) {
        updateMacro(sdk)
        ruffConfigService.projectRuffExecutablePath = findRuffExecutableInSDK(sdk, false)?.absolutePath?.let {
            ruffMacroService.collapseProjectExecutablePath(it)
        }
        ruffConfigService.projectRuffLspExecutablePath = findRuffExecutableInSDK(sdk, true)?.absolutePath?.let {
            ruffMacroService.collapseProjectExecutablePath(it)
        }
        ruffCacheService.setVersion()
        if (lspServerManager != null && ruffConfigService.useRuffLsp) {
            @Suppress("UnstableApiUsage")
            lspServerManager.stopAndRestartIfNeeded(RuffLspServerSupportProvider::class.java)
        }
    }

    private fun updateMacro(sdk: Sdk) {
        sdk.homeDirectory?.parent?.presentableUrl?.let {
            ruffMacroService.addMacroExpand(PY_INTERPRETER_DIRECTORY_MACRO_NAME, it)
        }
    }
}