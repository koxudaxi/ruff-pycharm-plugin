package com.koxudaxi.ruff

import RuffLspClientManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.packaging.common.PythonPackageManagementListener
import com.jetbrains.python.sdk.pythonSdk
import com.koxudaxi.ruff.lsp.ClientType

@Suppress("UnstableApiUsage")
class RuffPackageManagerListener(private val project: Project): PythonPackageManagementListener {

    override fun packagesChanged(sdk: Sdk) {
        if (project.pythonSdk != sdk) return
        val ruffConfigService = project.configService
        val ruffCacheService = RuffCacheService.getInstance(project)
        val ruffLspClientManager = RuffLspClientManager.getInstance(project)
        ruffCacheService.setProjectRuffExecutablePath(findRuffExecutableInSDK(sdk, false)?.absolutePath)
        ruffCacheService.setProjectRuffLspExecutablePath(findRuffExecutableInSDK(sdk, true)?.absolutePath)
        ruffCacheService.setVersion {
            if (!lspSupported || !ruffConfigService.enableLsp) return@setVersion
            ApplicationManager.getApplication().invokeLater {
                ApplicationManager.getApplication().runWriteAction {
                    when {
                        ruffLspClientManager.hasClient() -> ruffLspClientManager.restart()
                        else -> {
                            val clientType = when {
                                ruffConfigService.useLsp4ij -> ClientType.LSP4IJ
                                else -> ClientType.INTELLIJ
                            }
                            ruffLspClientManager.setClient(clientType)
                        }
                    }

                }
            }

        }
    }
}