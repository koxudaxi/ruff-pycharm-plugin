package com.koxudaxi.ruff

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.koxudaxi.ruff.lsp.ClientType
import javax.swing.JComponent


class RuffConfigurable internal constructor(val project: Project) : Configurable {
    private val configPanel: RuffConfigPanel = RuffConfigPanel(project)
    override fun getDisplayName(): String {
        return "Ruff"
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun createComponent(): JComponent {
        reset()
        return configPanel.configPanel
    }

    override fun reset() {}

    override fun isModified(): Boolean {
        val ruffConfigService: RuffConfigService = RuffConfigService.getInstance(project)
        return ruffConfigService.runRuffOnSave != configPanel.runRuffOnSave ||
                ruffConfigService.runRuffOnReformatCode != configPanel.runRuffOnReformatCode ||
                ruffConfigService.showRuleCode != configPanel.showRuleCode ||
                ruffConfigService.alwaysUseGlobalRuff != configPanel.alwaysUseGlobalRuff ||
                ruffConfigService.globalRuffExecutablePath != configPanel.globalRuffExecutablePath ||
                ruffConfigService.globalRuffLspExecutablePath != configPanel.globalRuffLspExecutablePath ||
                ruffConfigService.ruffConfigPath != configPanel.ruffConfigPath ||
                ruffConfigService.disableOnSaveOutsideOfProject != configPanel.disableOnSaveOutsideOfProject ||
                ruffConfigService.useRuffLsp != configPanel.useRuffLsp ||
                ruffConfigService.useIntellijLspClient != configPanel.useIntellijLspClient ||
                ruffConfigService.useLsp4ij != configPanel.useLsp4ij ||
                ruffConfigService.useRuffFormat != configPanel.useRuffFormat ||
                ruffConfigService.useRuffServer != configPanel.useRuffServer ||
                ruffConfigService.enableLsp != configPanel.enableLsp ||
                ruffConfigService.enableRuffLogging != configPanel.enableRuffLogging
    }

    override fun apply() {
        val ruffConfigService: RuffConfigService = RuffConfigService.getInstance(project)
        val ruffCacheService: RuffCacheService = RuffCacheService.getInstance(project)
        val ruffLspClientManager = RuffLspClientManager.getInstance(project)
        ruffConfigService.runRuffOnSave = configPanel.runRuffOnSave
        ruffConfigService.runRuffOnReformatCode = configPanel.runRuffOnReformatCode
        ruffConfigService.showRuleCode = configPanel.showRuleCode
        ruffConfigService.alwaysUseGlobalRuff = configPanel.alwaysUseGlobalRuff
        ruffConfigService.globalRuffExecutablePath = configPanel.globalRuffExecutablePath
        ruffConfigService.globalRuffLspExecutablePath = configPanel.globalRuffLspExecutablePath
        ruffConfigService.disableOnSaveOutsideOfProject = configPanel.disableOnSaveOutsideOfProject
        ruffConfigService.useRuffFormat = configPanel.useRuffFormat
        if (ruffConfigService.enableRuffLogging != configPanel.enableRuffLogging) {
            if (configPanel.enableRuffLogging) {
                // When logging is enabled:
                // 1. Show the "Ruff Logging" tool window so that the ConsoleView is registered.
                val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Ruff Logging")
                toolWindow?.show(null)
                // 2. Log a message indicating that logging has been enabled.
                RuffLoggingService.log(project, "Ruff logging has been enabled.")
            } else {
                // When logging is disabled:
                // 1. Log a message indicating that logging is being disabled.
                RuffLoggingService.log(project, "Ruff logging has been disabled.")
                // 2. Unregister the ConsoleView so that logging stops.
                RuffLoggingService.unregisterConsoleView(project)
            }
        }
        ruffConfigService.enableRuffLogging = configPanel.enableRuffLogging
        val ruffConfigPathChanged = configPanel.ruffConfigPath != configPanel.ruffConfigPath
        val useRuffLspChanged = ruffConfigService.useRuffLsp != configPanel.useRuffLsp
        val useRuffServerChanged = ruffConfigService.useRuffServer != configPanel.useRuffServer
        val lspClientChanged = (ruffConfigService.useIntellijLspClient != configPanel.useIntellijLspClient) ||
                (ruffConfigService.useLsp4ij != configPanel.useLsp4ij)
        ruffConfigService.ruffConfigPath = configPanel.ruffConfigPath
        ruffConfigService.useRuffLsp = configPanel.useRuffLsp
        ruffConfigService.useRuffServer = configPanel.useRuffServer
        ruffConfigService.useIntellijLspClient = configPanel.useIntellijLspClient
        ruffConfigService.useLsp4ij = configPanel.useLsp4ij
        val lspEnabledChanged = ruffConfigService.enableLsp != configPanel.enableLsp
        ruffConfigService.enableLsp = configPanel.enableLsp
        ruffCacheService.setVersion {
            if (!lspSupported) return@setVersion
            val startLsp = ruffConfigService.enableLsp
            if (lspClientChanged || (lspEnabledChanged && startLsp)) {
                if (ruffConfigService.useLsp4ij) {
                    ruffLspClientManager.setClient(ClientType.LSP4IJ, startLsp)
                } else {
                    ruffLspClientManager.setClient(ClientType.INTELLIJ, startLsp)
                }
            } else if (lspEnabledChanged) {
                ruffLspClientManager.stop()
            } else if (useRuffLspChanged || useRuffServerChanged || ruffConfigPathChanged) {
                if (startLsp) {
                    ruffLspClientManager.restart()
                }
            }
            // TODO: support ruff global executable path changed pattern
        }
    }

    override fun disposeUIResources() {
    }
}