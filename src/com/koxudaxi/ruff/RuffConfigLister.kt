package com.koxudaxi.ruff

import RuffLspServerSupportProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Key
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.serviceContainer.AlreadyDisposedException

class RuffConfigLister: EditorFactoryListener {
    private val changeListenerKey = Key.create<DocumentListener>("PyProjectToml.change.listener")

    override fun editorCreated(event: EditorFactoryEvent) {
        if (!lspIsSupported) return
        val project = event.editor.project
        if (project == null || !isRuffConfigEditor(event.editor)) return
        val listener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                try {
                    val document = event.document
                    if (FileDocumentManager.getInstance().isDocumentUnsaved(document)) {
                        if (RuffConfigService.getInstance(project).useRuffLsp) {
                            FileDocumentManager.getInstance().saveAllDocuments()
                            LspServerManager.getInstance(project)
                                .stopAndRestartIfNeeded(RuffLspServerSupportProvider::class.java)
                        }
                    }
                } catch (e: AlreadyDisposedException) {
                }
            }
        }
        with(event.editor.document) {
            addDocumentListener(listener)
            putUserData(changeListenerKey, listener)
        }
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        if (!lspIsSupported) return
        val listener = event.editor.getUserData(changeListenerKey) ?: return
        event.editor.document.removeDocumentListener(listener)
    }

    private fun isRuffConfigEditor(editor: Editor): Boolean {
        val file = editor.document.virtualFile ?: return false
        if (!file.isRuffConfig) return false
        val project = editor.project ?: return false
        return RuffConfigService.getInstance(project).useRuffLsp
        // TODO: Should detect if the file is in current project
        }
}