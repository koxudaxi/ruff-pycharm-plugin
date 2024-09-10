package com.koxudaxi.ruff

import RuffLspClientManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.packaging.PyPackageManager
import com.koxudaxi.ruff.lsp.ClientType

class RuffPackageManagerListener(project: Project) : PyPackageManager.Listener {
    private val ruffConfigService = RuffConfigService.getInstance(project)
    private val ruffCacheService = RuffCacheService.getInstance(project)
    private val ruffLspClientManager = RuffLspClientManager.getInstance(project)

    override fun packagesRefreshed(sdk: Sdk) {
        ruffConfigService.projectRuffExecutablePath = findRuffExecutableInSDK(sdk, false)?.absolutePath
        ruffConfigService.projectRuffLspExecutablePath = findRuffExecutableInSDK(sdk, true)?.absolutePath
        if (!lspSupported || !ruffConfigService.enableLsp) return
        ruffCacheService.setVersion {
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