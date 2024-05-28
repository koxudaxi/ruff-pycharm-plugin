package com.koxudaxi.ruff

import RuffLspServerSupportProvider
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerManager
import javax.swing.JComponent


class RuffConfigurable internal constructor(val project: Project) : Configurable {
    private val ruffConfigService: RuffConfigService = RuffConfigService.getInstance(project)
    private val configPanel: RuffConfigPanel = RuffConfigPanel(project)
    private val ruffCacheService: RuffCacheService = RuffCacheService.getInstance(project)
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
                ruffConfigService.useRuffFormat != configPanel.useRuffFormat

    }

    override fun apply() {
        ruffConfigService.runRuffOnSave = configPanel.runRuffOnSave
        ruffConfigService.runRuffOnReformatCode = configPanel.runRuffOnReformatCode
        ruffConfigService.showRuleCode = configPanel.showRuleCode
        ruffConfigService.alwaysUseGlobalRuff = configPanel.alwaysUseGlobalRuff
        ruffConfigService.globalRuffExecutablePath = configPanel.globalRuffExecutablePath
        ruffConfigService.globalRuffLspExecutablePath = configPanel.globalRuffLspExecutablePath
        ruffConfigService.ruffConfigPath = configPanel.ruffConfigPath
        ruffConfigService.disableOnSaveOutsideOfProject = configPanel.disableOnSaveOutsideOfProject
        ruffConfigService.useRuffFormat = configPanel.useRuffFormat
        ruffCacheService.setVersion()
        if (ruffConfigService.useRuffLsp != configPanel.useRuffLsp) {

        }
        if (configPanel.ruffConfigPath != configPanel.ruffConfigPath) {}
    }


    private fun startIntellijLspClient() {
        @Suppress("UnstableApiUsage")
        val lspServerManager = if (intellijLspClientSupported) LspServerManager.getInstance(project) else null
        if (lspServerManager != null) {
            if (configPanel.useRuffLsp) {
                @Suppress("UnstableApiUsage")
                lspServerManager.startServersIfNeeded(RuffLspServerSupportProvider::class.java)
            } else {
                @Suppress("UnstableApiUsage")
                lspServerManager.stopServers(RuffLspServerSupportProvider::class.java)
            }
        }
        ruffConfigService.useRuffLsp = configPanel.useRuffLsp
    }

    override fun disposeUIResources() {
    }
}