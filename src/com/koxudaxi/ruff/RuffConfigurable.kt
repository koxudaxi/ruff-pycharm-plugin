package com.koxudaxi.ruff

import RuffLspServerSupportProvider
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.koxudaxi.ruff.lsp.ClientType
import com.koxudaxi.ruff.lsp.lsp4ij.RuffLsp4IntellijClient
import javax.swing.JComponent


@Suppress("UnstableApiUsage")
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
        val ruffConfigService: RuffConfigService = project.configService
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
                ruffConfigService.enableRuffLogging != configPanel.enableRuffLogging ||
                ruffConfigService.codeActionFeature != configPanel.codeActionFeature ||
                ruffConfigService.diagnosticFeature != configPanel.diagnosticFeature ||
                ruffConfigService.formattingFeature != configPanel.formattingFeature ||
                ruffConfigService.hoverFeature != configPanel.hoverFeature


    }

    override fun apply() {
        val ruffConfigService: RuffConfigService = project.configService
        val ruffCacheService: RuffCacheService = RuffCacheService.getInstance(project)
        val ruffLspClientManager = RuffLspClientManager.getInstance(project)

        val wasUsingLsp4ij = ruffConfigService.useLsp4ij
        val willUseLsp4ij = configPanel.useLsp4ij

        val wasLspEnabled = ruffConfigService.enableLsp
        val wasUsingRuffLsp = ruffConfigService.useRuffLsp
        val wasUsingRuffServer = ruffConfigService.useRuffServer

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
                val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Ruff Logging")
                toolWindow?.show(null)
                RuffLoggingService.log(project, "Ruff logging has been enabled.")
            } else {
                RuffLoggingService.log(project, "Ruff logging has been disabled.")
                RuffLoggingService.unregisterConsoleView(project)
            }
        }
        ruffConfigService.enableRuffLogging = configPanel.enableRuffLogging

        val ruffConfigPathChanged = ruffConfigService.ruffConfigPath != configPanel.ruffConfigPath
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
        val lspFeatureChanged = ruffConfigService.codeActionFeature != configPanel.codeActionFeature ||
                ruffConfigService.diagnosticFeature != configPanel.diagnosticFeature ||
                ruffConfigService.formattingFeature != configPanel.formattingFeature ||
                ruffConfigService.hoverFeature != configPanel.hoverFeature

        ruffConfigService.enableLsp = configPanel.enableLsp
        ruffConfigService.codeActionFeature = configPanel.codeActionFeature
        ruffConfigService.diagnosticFeature = configPanel.diagnosticFeature
        ruffConfigService.formattingFeature = configPanel.formattingFeature
        ruffConfigService.hoverFeature = configPanel.hoverFeature

        if (wasUsingLsp4ij != willUseLsp4ij) {
            RuffLoggingService.log(project, "LSP client type changed, shutting down previous client")

            if (wasUsingLsp4ij) {
                try {
                    val languageServerManager = com.redhat.devtools.lsp4ij.LanguageServerManager.getInstance(project)
                    languageServerManager.stop(RuffLsp4IntellijClient.LANGUAGE_SERVER_ID)
                    RuffLoggingService.log(project, "Stopped LSP4IJ client")
                } catch (e: Exception) {
                    RuffLoggingService.log(project, "Error stopping LSP4IJ client: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
                }
            } else {
                try {
                    com.intellij.platform.lsp.api.LspServerManager.getInstance(project)
                        .stopServers(RuffLspServerSupportProvider::class.java)
                    RuffLoggingService.log(project, "Stopped IntelliJ LSP client")
                } catch (e: Exception) {
                    RuffLoggingService.log(project, "Error stopping IntelliJ LSP client: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
                }
            }

            ruffLspClientManager.stop()
        }

        ruffCacheService.setVersion {
            if (!lspSupported) return@setVersion

            val startLsp = ruffConfigService.enableLsp
            val lspSettingsChanged = wasLspEnabled != startLsp ||
                    wasUsingRuffLsp != ruffConfigService.useRuffLsp ||
                    wasUsingRuffServer != ruffConfigService.useRuffServer ||
                    wasUsingLsp4ij != willUseLsp4ij

            if (startLsp && lspSettingsChanged) {
                RuffLoggingService.log(project, "Initializing new LSP client")
                if (ruffConfigService.useLsp4ij) {
                    ruffLspClientManager.setClient(ClientType.LSP4IJ, true)
                } else {
                    ruffLspClientManager.setClient(ClientType.INTELLIJ, true)
                }
            } else if (!startLsp && wasLspEnabled) {
                ruffLspClientManager.stop()
            } else if (useRuffLspChanged || useRuffServerChanged || ruffConfigPathChanged || lspFeatureChanged) {
                if (startLsp) {
                    ruffLspClientManager.restart()
                }
            }
        }
    }

    override fun disposeUIResources() {
    }
}