package com.koxudaxi.ruff

import RuffLspServerSupportProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.lsp.api.LspServerManager
import com.jetbrains.python.packaging.PyPackageManager

class RuffPackageManagerListener(project: Project) : PyPackageManager.Listener {
    private val ruffConfigService = RuffConfigService.getInstance(project)
    private val ruffCacheService = RuffCacheService.getInstance(project)
    @Suppress("UnstableApiUsage")
    private val lspServerManager = if (lspIsSupported) LspServerManager.getInstance(project) else null
    override fun packagesRefreshed(sdk: Sdk) {
        ruffConfigService.projectRuffExecutablePath = findRuffExecutableInSDK(sdk, false)?.absolutePath
        ruffConfigService.projectRuffLspExecutablePath = findRuffExecutableInSDK(sdk, true)?.absolutePath
        ruffCacheService.setVersion()
        if (lspServerManager != null && ruffConfigService.useRuffLsp) {
            try {
                @Suppress("UnstableApiUsage")
                lspServerManager.stopAndRestartIfNeeded(RuffLspServerSupportProvider::class.java)
            } catch (_: Exception) {
            }
         }
    }
}