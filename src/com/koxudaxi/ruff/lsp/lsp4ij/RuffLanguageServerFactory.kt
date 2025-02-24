package com.koxudaxi.ruff.lsp.lsp4ij

import com.intellij.execution.ui.ConsoleViewContentType
import com.koxudaxi.ruff.lsp.lsp4ij.features.*
import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.*
import com.redhat.devtools.lsp4ij.LanguageServerEnablementSupport
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.client.features.*
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider

@Suppress("UnstableApiUsage")
class RuffLanguageServerFactory : LanguageServerFactory, LanguageServerEnablementSupport {
    private var enabled = true

    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return RuffLanguageServer(project)

    }

    //If you need to provide client specific features
    override fun createLanguageClient(project: Project): LanguageClientImpl {
        return RuffLanguageClient(project)
    }

    override fun isEnabled(project: Project): Boolean {
        try {
            if (!enabled) {
                return false
            }

            val ruffConfigService = project.configService
            if (!ruffConfigService.enableLsp) {
                RuffLoggingService.log(project, "LSP not enabled in config", ConsoleViewContentType.NORMAL_OUTPUT)
                return false
            }
            if (!ruffConfigService.useRuffLsp && !ruffConfigService.useRuffServer) {
                RuffLoggingService.log(
                    project,
                    "Neither Ruff LSP nor Server enabled",
                    ConsoleViewContentType.NORMAL_OUTPUT
                )
                return false
            }
            if (!ruffConfigService.useLsp4ij) {
                RuffLoggingService.log(project, "LSP4IJ not enabled in config", ConsoleViewContentType.NORMAL_OUTPUT)
                return false
            }
            if (!isInspectionEnabled(project)) {
                RuffLoggingService.log(project, "Ruff inspection not enabled", ConsoleViewContentType.NORMAL_OUTPUT)
                return false
            }

            val ruffCacheService = RuffCacheService.getInstance(project)

            val version = ruffCacheService.getOrPutVersion().getNow(null)
            if (version == null) {
                RuffLoggingService.log(project, "Ruff version is not yet determined. Skipping LSP enable check.", ConsoleViewContentType.NORMAL_OUTPUT)
                return false
            }

            // Additional checks
            if (ruffConfigService.useRuffServer && ruffCacheService.hasLsp() != true) {
                if (ruffCacheService.hasLsp() == false) {
                    RuffLoggingService.log(
                        project,
                        "Server mode enabled but Ruff version doesn't support LSP",
                        ConsoleViewContentType.NORMAL_OUTPUT
                    )
                    return false
                } else {
                    RuffLoggingService.log(
                        project,
                        "Server mode enabled but Ruff version is not yet determined. Skipping LSP enable check.",
                        ConsoleViewContentType.NORMAL_OUTPUT
                    )
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
                RuffLoggingService.log(project, "Ruff executable not found", ConsoleViewContentType.NORMAL_OUTPUT)
                return false
            }

            return true
        } catch (e: Exception) {
            RuffLoggingService.log(
                project,
                "Error checking LSP enablement: ${e.message}",
                ConsoleViewContentType.ERROR_OUTPUT
            )
            return false
        }
    }

    override fun setEnabled(isEnabled: Boolean, project: Project) {
        this.enabled = isEnabled
        if (!isEnabled) {
            RuffLoggingService.log(
                project,
                "Ruff LSP4IJ setEnabled(false) -- Doing nothing here, letting LSP4IJ handle stop.",
                ConsoleViewContentType.NORMAL_OUTPUT
            )
        }
    }


    override fun createClientFeatures(): LSPClientFeatures {
        return LSPClientFeatures().setCodeActionFeature(RuffLSPCodeActionFeature())
            .setDiagnosticFeature(RuffLSPDiagnosticFeature())
            .setFormattingFeature(RuffLSPFormattingFeature())
            .setHoverFeature(RuffLSPHoverFeature())

    }
}