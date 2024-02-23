package com.koxudaxi.ruff

import RuffLspServerSupportProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.lsp.api.LspServerManager
import com.jetbrains.python.packaging.PyPackageManager
import java.io.File

class RuffPackageManagerListener(project: Project) : PyPackageManager.Listener {
    private val ruffConfigService = RuffConfigService.getInstance(project)
    private val ruffCacheService = RuffCacheService.getInstance(project)
    @Suppress("UnstableApiUsage")
    private val lspServerManager = if (lspIsSupported) LspServerManager.getInstance(project) else null
    override fun packagesRefreshed(sdk: Sdk) {
        fun collapse(file: File?): String? {
            return file?.absolutePath?.replace(sdk.homeDirectory!!.parent.presentableUrl, "\$PyInterpreterDirectory$")
        }
        ruffConfigService.projectRuffExecutablePath = collapse(findRuffExecutableInSDK(sdk, false))
        ruffConfigService.projectRuffLspExecutablePath = findRuffExecutableInSDK(sdk, true)?.absolutePath
        ruffCacheService.setVersion()
        if (lspServerManager != null && ruffConfigService.useRuffLsp) {
            @Suppress("UnstableApiUsage")
            lspServerManager.stopAndRestartIfNeeded(RuffLspServerSupportProvider::class.java)
        }
    }
}