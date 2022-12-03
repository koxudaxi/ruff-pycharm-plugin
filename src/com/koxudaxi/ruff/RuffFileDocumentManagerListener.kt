package com.koxudaxi.ruff

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project

class RuffFileDocumentManagerListener(private val project: Project) : FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        if (!RuffConfigService.getInstance(project).runRuffOnSave) {
            return
        }
        val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
        if (!virtualFile.isPyFile) return
        val fileName = virtualFile.name
        val module = ModuleUtil.findModuleForFile(virtualFile, project) ?: return
        val args = listOf("--fix", "--stdin-filename", fileName, "-")
        val stdin = document.text.toByteArray(virtualFile.charset)
        runRuffInBackground(module, stdin, args, "running ruff $fileName") {
            if (it is String) {
                runInEdt {
                    runWriteAction {
                        document.setText(it)
                    }
                }
            }
        }
    }
}