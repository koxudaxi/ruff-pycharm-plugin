package com.koxudaxi.ruff

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class RuffLoggingToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val consoleView: ConsoleView = ConsoleViewImpl(project, false)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(consoleView.component, "Console", false)
        toolWindow.contentManager.addContent(content)

        consoleView.print("Ruff Plugin Console Initialized\n", ConsoleViewContentType.NORMAL_OUTPUT)
        // Show current logging settings
        RuffConfigService.getInstance(project).let {
            consoleView.print("Logging enabled: ${it.enableRuffLogging}\n", ConsoleViewContentType.NORMAL_OUTPUT)
        }
        RuffLoggingService.registerConsoleView(project, consoleView)
    }
}
