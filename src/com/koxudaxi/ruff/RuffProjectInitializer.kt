package com.koxudaxi.ruff

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
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
            } catch (_: AlreadyDisposedException) {
            }
        }
    }
}
