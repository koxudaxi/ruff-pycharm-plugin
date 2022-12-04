package com.koxudaxi.ruff

import com.intellij.execution.Location
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.modules
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.python.sdk.isAssociatedWithModule
import com.jetbrains.python.sdk.pythonSdk


class RuffRun : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val pythonSdk = project.pythonSdk ?: return
        val targetFile = e.getData(Location.DATA_KEY)?.virtualFile?.path ?: project.basePath ?: return
        project.modules.firstOrNull { pythonSdk.isAssociatedWithModule(it) }
            ?.let {
                runRuffInBackground(it, null, listOf("--exit-zero", "--fix", targetFile), "running ruff") {
                    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
                }
            }
    }

    init {
        templatePresentation.icon = AllIcons.Actions.Execute
    }

    companion object {
        const val actionID = "runRuff"
    }
}