package com.koxudaxi.ruff.lsp.lsp4ij

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.RuffLoggingService
import com.koxudaxi.ruff.configService
import com.koxudaxi.ruff.lsp.ClientType
import com.koxudaxi.ruff.lsp.RuffLspClient
import com.koxudaxi.ruff.lsp4ijSupported
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.redhat.devtools.lsp4ij.LanguageServersRegistry
import com.redhat.devtools.lsp4ij.ServerStatus
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RuffLsp4IntellijClient(val project: Project) : RuffLspClient {
    private val languageServerManager = if(lsp4ijSupported) LanguageServerManager.getInstance(project) else null

    @Volatile
    private var starting = AtomicBoolean(false)

    @Volatile
    private var stopping = AtomicBoolean(false)

    private val isRunning: Boolean
        get() {
            if (languageServerManager == null) return false
            try {
                val status = languageServerManager.getServerStatus(LANGUAGE_SERVER_ID)
                return status == ServerStatus.started || status == ServerStatus.starting
            } catch (e: Exception) {
                RuffLoggingService.log(project, "Error checking LSP4IJ server status: ${e.message}",
                    ConsoleViewContentType.ERROR_OUTPUT)
                return false
            }
        }

    override fun start() {
        if (languageServerManager == null) return

        if (!starting.compareAndSet(false, true)) {
            RuffLoggingService.log(project, "LSP4IJ start already in progress", ConsoleViewContentType.NORMAL_OUTPUT)
            return
        }

        try {
            val ruffConfigService = project.configService
            ruffConfigService.useLsp4ij = true

            safeStop()

            val registry = LanguageServersRegistry.getInstance()
            val serverDefinition = registry.getServerDefinition(LANGUAGE_SERVER_ID)

            if (serverDefinition != null) {
                serverDefinition.setEnabled(true, project)

                RuffLoggingService.log(project, "Starting LSP4IJ client", ConsoleViewContentType.NORMAL_OUTPUT)

                val startLatch = CountDownLatch(1)
                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        languageServerManager.start(LANGUAGE_SERVER_ID,
                            LanguageServerManager.StartOptions()
                                .setForceStart(true)
                                .setWillEnable(true))

                        var attempts = 0
                        while (attempts < 20) {
                            attempts++
                            if (languageServerManager.getServerStatus(LANGUAGE_SERVER_ID) == ServerStatus.started) {
                                break
                            }
                            Thread.sleep(100)
                        }
                    } catch (e: Exception) {
                        RuffLoggingService.log(project, "Error in LSP4IJ start thread: ${e.message}",
                            ConsoleViewContentType.ERROR_OUTPUT)
                    } finally {
                        startLatch.countDown()
                    }
                }

                startLatch.await(5, TimeUnit.SECONDS)
            } else {
                RuffLoggingService.log(project, "LSP4IJ server definition not found",
                    ConsoleViewContentType.ERROR_OUTPUT)
            }
        } catch (e: Exception) {
            RuffLoggingService.log(project, "Error starting LSP4IJ client: ${e.message}",
                ConsoleViewContentType.ERROR_OUTPUT)
        } finally {
            starting.set(false)
        }
    }

    override fun stop() {
        if (languageServerManager == null) return
        safeStop()
    }

    private fun safeStop() {
        if (!stopping.compareAndSet(false, true)) {
            RuffLoggingService.log(project, "LSP4IJ stop already in progress", ConsoleViewContentType.NORMAL_OUTPUT)
            return
        }

        try {
            RuffLoggingService.log(project, "Stopping LSP4IJ client", ConsoleViewContentType.NORMAL_OUTPUT)

            val stopOptions = LanguageServerManager.StopOptions()
                .setWillDisable(true)

            try {
                languageServerManager?.stop(LANGUAGE_SERVER_ID, stopOptions)

                val stopLatch = CountDownLatch(1)
                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        var attempts = 0
                        while (attempts < 20) {
                            attempts++
                            if (languageServerManager?.getServerStatus(LANGUAGE_SERVER_ID) == ServerStatus.stopped) {
                                break
                            }
                            Thread.sleep(100)
                        }
                    } finally {
                        stopLatch.countDown()
                    }
                }

                stopLatch.await(5, TimeUnit.SECONDS)
            } catch (e: Exception) {
                RuffLoggingService.log(project, "Error stopping LSP4IJ client: ${e.message}",
                    ConsoleViewContentType.ERROR_OUTPUT)

                val registry = LanguageServersRegistry.getInstance()
                val serverDefinition = registry.getServerDefinition(LANGUAGE_SERVER_ID)
                serverDefinition?.setEnabled(false, project)
            }
        } catch (e: Exception) {
            RuffLoggingService.log(project, "Error stopping LSP4IJ client: ${e.message}",
                ConsoleViewContentType.ERROR_OUTPUT)
        } finally {
            stopping.set(false)
        }
    }

    override fun restart() {
        if (languageServerManager == null) return

        try {
            RuffLoggingService.log(project, "Restarting LSP4IJ client", ConsoleViewContentType.NORMAL_OUTPUT)

            val registry = LanguageServersRegistry.getInstance()
            val serverDefinition = registry.getServerDefinition(LANGUAGE_SERVER_ID)

            stop()

            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    Thread.sleep(300)

                    serverDefinition?.setEnabled(true, project)
                    start()
                } catch (e: Exception) {
                    RuffLoggingService.log(project, "Error in restart thread: ${e.message}",
                        ConsoleViewContentType.ERROR_OUTPUT)
                }
            }
        } catch (e: Exception) {
            RuffLoggingService.log(project, "Error restarting LSP4IJ client: ${e.message}",
                ConsoleViewContentType.ERROR_OUTPUT)
        }
    }

    override fun getClientType(): ClientType {
        return ClientType.LSP4IJ
    }

    companion object {
        const val LANGUAGE_SERVER_ID = "ruffLanguageServer"

    }
}