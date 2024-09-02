package com.koxudaxi.ruff

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.lsp.intellij.RuffIntellijLspClient
import com.koxudaxi.ruff.lsp.lsp4ij.RuffLsp4IntellijClient
import javax.swing.JComponent


class RuffConfigurable internal constructor(val project: Project) : Configurable {
    private val ruffConfigService: RuffConfigService = RuffConfigService.getInstance(project)
    private val configPanel: RuffConfigPanel = RuffConfigPanel(project)
    private val ruffCacheService: RuffCacheService = RuffCacheService.getInstance(project)
    private val ruffLspClientManager = RuffLspClientManager.getInstance(project)
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
                ruffConfigService.enableLsp != configPanel.enableLsp
    }

    override fun apply() {
        ruffConfigService.runRuffOnSave = configPanel.runRuffOnSave
        ruffConfigService.runRuffOnReformatCode = configPanel.runRuffOnReformatCode
        ruffConfigService.showRuleCode = configPanel.showRuleCode
        ruffConfigService.alwaysUseGlobalRuff = configPanel.alwaysUseGlobalRuff
        ruffConfigService.globalRuffExecutablePath = configPanel.globalRuffExecutablePath
        ruffConfigService.globalRuffLspExecutablePath = configPanel.globalRuffLspExecutablePath
        ruffConfigService.disableOnSaveOutsideOfProject = configPanel.disableOnSaveOutsideOfProject
        ruffConfigService.useRuffFormat = configPanel.useRuffFormat
        val ruffConfigPathChanged = configPanel.ruffConfigPath != configPanel.ruffConfigPath
        val useRuffLspChanged = ruffConfigService.useRuffLsp != configPanel.useRuffLsp
        val useRuffServerChanged = ruffConfigService.useRuffServer != configPanel.useRuffServer
        val lspClientChanged = ruffConfigService.useIntellijLspClient != configPanel.useIntellijLspClient
        ruffConfigService.ruffConfigPath = configPanel.ruffConfigPath
        ruffConfigService.useRuffLsp = configPanel.useRuffLsp
        ruffConfigService.useRuffServer = configPanel.useRuffServer
        ruffConfigService.useIntellijLspClient = configPanel.useIntellijLspClient
        ruffConfigService.useLsp4ij = configPanel.useLsp4ij
        ruffConfigService.enableLsp = configPanel.enableLsp
        ruffCacheService.setVersion {
            val startLsp = ruffConfigService.enableLsp
            var started = false
            if (lspClientChanged) {
                if (ruffConfigService.useLsp4ij) {
                    ruffLspClientManager.setClient(RuffLsp4IntellijClient::class, startLsp)
                } else {
                    ruffLspClientManager.setClient(RuffIntellijLspClient::class, startLsp)
                }
                started = startLsp
            } else {
                if (useRuffLspChanged || useRuffServerChanged) {
                    if (startLsp) {
                        ruffLspClientManager.start()
                        started = true
                    } else {
                        ruffLspClientManager.stop()
                    }
                }
            }

            if (!started && ruffConfigPathChanged && startLsp) {
                    ruffLspClientManager.restart()
            }
            // TODO: support ruff global executable path changed pattern
        }
    }

    override fun disposeUIResources() {
    }
}