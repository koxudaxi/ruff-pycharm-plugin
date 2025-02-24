package com.koxudaxi.ruff

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.serviceContainer.AlreadyDisposedException
import com.koxudaxi.ruff.lsp.ClientType

class RuffProjectInitializer : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) return
        if (project.isDisposed) return

        DumbService.getInstance(project).smartInvokeLater {
            try {
                if (project.isDisposed) return@smartInvokeLater

                RuffCacheService.getInstance(project).getOrPutVersion().thenAccept { version ->
                    if (project.isDisposed) return@thenAccept

                    if (lspSupported) {
                        val ruffConfigService = project.configService
                        if (ruffConfigService.enableLsp) {
                            ApplicationManager.getApplication().invokeLater {
                                if (project.isDisposed) return@invokeLater

                                val ruffLspClientManager = RuffLspClientManager.getInstance(project)
                                if (ruffConfigService.useLsp4ij) {
                                    ruffLspClientManager.setClient(ClientType.LSP4IJ, true)
                                } else {
                                    ruffLspClientManager.setClient(ClientType.INTELLIJ, true)
                                }

                                RuffLoggingService.log(project, "LSP client initialized during project startup")
                            }
                        }

                        setUpPyProjectTomlLister(project)
                    }
                }
            } catch (_: AlreadyDisposedException) {
            } catch (e: Exception) {
                RuffLoggingService.log(project, "Error initializing Ruff: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
            }
        }
    }

    private fun setUpPyProjectTomlLister(project: Project) {
        VirtualFileManager.getInstance().addAsyncFileListener(
            { events ->
                object : AsyncFileListener.ChangeApplier {
                    override fun afterVfsChange() {
                        if (project.isDisposed) return
                        try {
                            if (!events
                                    .filter { it is VFileContentChangeEvent || it is VFileMoveEvent || it is VFileCopyEvent || it is VFileCreateEvent || it is VFileDeleteEvent }
                                    .mapNotNull { it.file }
                                    .filter { it.isRuffConfig }
                                    .any {
                                        try {
                                            ProjectFileIndex.getInstance(project).isInContent(it)
                                        } catch (e: Exception) {
                                            false
                                        }
                                    }
                            ) return
                            ApplicationManager.getApplication().invokeLater {
                                if (project.isDisposed) return@invokeLater
                                val ruffLspClientManager = RuffLspClientManager.getInstance(project)
                                if (!ruffLspClientManager.hasClient()) return@invokeLater
                                if (!project.configService.enableLsp) {
                                    ruffLspClientManager.stop()
                                } else {
                                    ruffLspClientManager.restart()
                                }
                            }
                        } catch (_: AlreadyDisposedException) {
                        }
                    }
                }
            },
            {}
        )
    }
}
