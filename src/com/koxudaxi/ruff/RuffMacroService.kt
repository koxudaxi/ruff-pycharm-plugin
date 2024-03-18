package com.koxudaxi.ruff
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.python.sdk.pythonSdk
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class RuffMacroService(private val project: Project) {
    private val macroManager = PathMacroManager.getInstance(project).expandMacroMap

    fun addMacroExpand(macroName: String, path: String) {
        macroManager.addMacroExpand(macroName, path)
    }
    fun collapseProjectExecutablePath(path: String): String {
        return path.replace(project.pythonSdk?.homeDirectory!!.parent.presentableUrl, "\$${PY_INTERPRETER_DIRECTORY_MACRO_NAME}$")
    }
    fun expandProjectExecutablePath(path: String): String {
        return macroManager.substitute(path, false)
    }
    companion object {
        fun getInstance(project: Project): RuffMacroService {
            return project.getService(RuffMacroService::class.java)
        }
    }
}