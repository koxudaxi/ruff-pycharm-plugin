package com.koxudaxi.ruff

import RuffLspServerSupportProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.serviceContainer.AlreadyDisposedException

class RuffProjectInitializer : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) return
        if (project.isDisposed) return
        DumbService.getInstance(project).smartInvokeLater {
            try {
                val ruffCacheService = RuffCacheService.getInstance(project)
                if (ruffCacheService.getVersion() == null) {
                    ruffCacheService.setVersion()
                }
                if (lspIsSupported) {
                    setUpPyProjectTomlLister(project)
                }
            } catch (_: AlreadyDisposedException) {
            }
        }
    }

    private fun setUpPyProjectTomlLister(project: Project) {
        val ruffConfigService = RuffConfigService.getInstance(project)
        @Suppress("UnstableApiUsage")
        val lspServerManager = LspServerManager.getInstance(project)

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
                                if (!ruffConfigService.useRuffLsp) return@invokeLater
                                lspServerManager.stopAndRestartIfNeeded(RuffLspServerSupportProvider::class.java)

                                LspServerManager.getInstance(project)
                                    .stopAndRestartIfNeeded(RuffLspServerSupportProvider::class.java)
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
