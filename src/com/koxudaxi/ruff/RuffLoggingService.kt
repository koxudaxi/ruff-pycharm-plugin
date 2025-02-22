package com.koxudaxi.ruff

import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project

object RuffLoggingService {
    private val consoleMap = mutableMapOf<Project, ConsoleView>()

    // Register a ConsoleView for the given project
    fun registerConsoleView(project: Project, consoleView: ConsoleView) {
        consoleMap[project] = consoleView
    }

    // Unregister the ConsoleView when it is no longer needed
    fun unregisterConsoleView(project: Project) {
        consoleMap.remove(project)
    }

    // Log a message to the ConsoleView for the specified project
    fun log(project: Project, message: String, contentType: ConsoleViewContentType = ConsoleViewContentType.NORMAL_OUTPUT) {
        if (!project.configService.enableRuffLogging) {
            return
        }
        consoleMap[project]?.print("$message\n", contentType)
    }
}
