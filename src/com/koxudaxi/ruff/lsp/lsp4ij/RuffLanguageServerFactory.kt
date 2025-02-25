package com.koxudaxi.ruff.lsp.lsp4ij

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.*
import com.koxudaxi.ruff.lsp.lsp4ij.features.*
import com.redhat.devtools.lsp4ij.LanguageServerEnablementSupport
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.client.features.*
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("UnstableApiUsage")
class RuffLanguageServerFactory : LanguageServerFactory, LanguageServerEnablementSupport {
    private var enabled = true
    private var logDisabled = AtomicBoolean(false)

    private var lastDisableReason: String? = null

    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return RuffLanguageServer(project)

    }

    override fun createLanguageClient(project: Project): LanguageClientImpl {
        return RuffLanguageClient(project)
    }

    override fun isEnabled(project: Project): Boolean {
        try {
            if (!enabled) {
                if (logDisabled.compareAndSet(false, true)) {
                    RuffLoggingService.log(project, "LSP4IJ is explicitly disabled", ConsoleViewContentType.NORMAL_OUTPUT)
                }
                return false
            }

            logDisabled.set(false)

            val ruffConfigService = project.configService
            if (!ruffConfigService.enableLsp) {
                setDisableReason(project, "LSP not enabled in config")
                return false
            }
            if (!ruffConfigService.useRuffLsp && !ruffConfigService.useRuffServer) {
                setDisableReason(project, "Neither Ruff LSP nor Server enabled")
                return false
            }
            if (!ruffConfigService.useLsp4ij) {
                setDisableReason(project, "LSP4IJ not enabled in config")
                return false
            }
            if (!isInspectionEnabled(project)) {
                setDisableReason(project, "Ruff inspection not enabled")
                return false
            }

            val ruffCacheService = RuffCacheService.getInstance(project)

            val version = ruffCacheService.getVersion()
            if (version == null) {
                setDisableReason(project, "Ruff version is not yet determined. Skipping LSP enable check.")
                return false
            }

            if (ruffConfigService.useRuffServer && ruffCacheService.hasLsp() != true) {
                if (ruffCacheService.hasLsp() == false) {
                    setDisableReason(project, "Server mode enabled but Ruff version doesn't support LSP")
                    return false
                } else {
                    setDisableReason(project, "Server mode enabled but Ruff version is not yet determined. Skipping LSP enable check.")
                    return false
                }
            }

            val executable = getRuffExecutable(
                project,
                ruffConfigService,
                ruffConfigService.useRuffLsp,
                ruffCacheService
            )
            if (executable == null) {
                setDisableReason(project, "Ruff executable not found")
                return false
            }

            lastDisableReason = null
            return true
        } catch (e: Exception) {
            RuffLoggingService.log(project, "Error checking LSP enablement: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
            return false
        }
    }

    private fun setDisableReason(project: Project, reason: String) {
        if (lastDisableReason != reason) {
            RuffLoggingService.log(project, reason, ConsoleViewContentType.NORMAL_OUTPUT)
            lastDisableReason = reason
        }
    }

    override fun setEnabled(isEnabled: Boolean, project: Project) {
        RuffLoggingService.log(project, "RuffLanguageServerFactory.setEnabled($isEnabled) called", ConsoleViewContentType.NORMAL_OUTPUT)

        val oldEnabled = enabled
        enabled = isEnabled

        if (oldEnabled != isEnabled) {
            logDisabled.set(false)
        }

        if (!isEnabled && oldEnabled) {
            try {
                val languageServerManager = LanguageServerManager.getInstance(project)
                RuffLoggingService.log(project, "Forcibly stopping LSP4IJ server", ConsoleViewContentType.NORMAL_OUTPUT)
                languageServerManager.stop(RuffLsp4IntellijClient.LANGUAGE_SERVER_ID,
                    LanguageServerManager.StopOptions().setWillDisable(true))
            } catch (e: Exception) {
                RuffLoggingService.log(project, "Error stopping LSP4IJ server: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
            }
        }
    }

    override fun createClientFeatures(): LSPClientFeatures {
        return LSPClientFeatures().setCodeActionFeature(RuffLSPCodeActionFeature())
            .setDiagnosticFeature(RuffLSPDiagnosticFeature())
            .setFormattingFeature(RuffLSPFormattingFeature())
            .setHoverFeature(RuffLSPHoverFeature())

    }
}