package com.koxudaxi.ruff

import com.intellij.application.options.ReplacePathToMacroMap
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.python.sdk.PySdkSettings
import com.jetbrains.python.sdk.pythonSdk
import org.jetbrains.annotations.SystemDependent
import org.jetbrains.jps.model.serialization.PathMacroUtil


@State(name = "RuffConfigService", storages = [Storage("ruff.xml")])
@Service(Service.Level.PROJECT)
class RuffConfigService : PersistentStateComponent<RuffConfigService> {
    var runRuffOnSave: Boolean = false
    var runRuffOnReformatCode: Boolean = true
    var showRuleCode: Boolean = true
    var globalRuffExecutablePath: @SystemDependent String? = null
    var globalRuffLspExecutablePath: @SystemDependent String? = null
    var alwaysUseGlobalRuff: Boolean = false
    var projectRuffExecutablePath: @SystemDependent String? = null
    var projectRuffLspExecutablePath: @SystemDependent String? = null
    var ruffConfigPath: @SystemDependent String? = null
    var disableOnSaveOutsideOfProject: Boolean = true
    var useRuffLsp: Boolean = false
    var useRuffFormat: Boolean = false

    private val Sdk.pythonInterpreterDirectory: String?
        get() =  homeDirectory?.parent?.presentableUrl


    fun setProjectRuffExecutablePath(path: @SystemDependent String, sdk: Sdk) {
        val pathMap = ReplacePathToMacroMap().apply {
            sdk.pythonInterpreterDirectory?.let {
                addMacroReplacement(it, PY_INTERPRETER_DIRECTORY_MACRO_NAME)
            }
        }
        projectRuffExecutablePath = pathMap.substitute(path, true)
    }
    fun getProjectRuffExecutablePath(sdk: Sdk?): @SystemDependent String? {
        val rawProjectRuffExecutablePath = projectRuffExecutablePath ?: return null
        // TODO: cache ruff executable path for same sdk
        val pathMap = ExpandMacroToPathMap().apply {
            sdk?.pythonInterpreterDirectory?.let {
                addMacroExpand(PY_INTERPRETER_DIRECTORY_MACRO_NAME, it)
            }
        }
        return pathMap.substitute(rawProjectRuffExecutablePath, true)
    }

    fun setProjectRuffLspExecutablePath(path: @SystemDependent String, sdk: Sdk) {
        val pathMap = ReplacePathToMacroMap().apply {
            sdk.pythonInterpreterDirectory?.let {
                addMacroReplacement(it, PY_INTERPRETER_DIRECTORY_MACRO_NAME)
            }
        }
        projectRuffLspExecutablePath = pathMap.substitute(path, true)
    }
    fun getProjectRuffLspExecutablePath(sdk: Sdk?): @SystemDependent String? {
        val rawProjectRuffLspExecutablePath = projectRuffLspExecutablePath ?: return null
        // TODO: cache ruff executable path for same sdk
        val pathMap = ExpandMacroToPathMap().apply {
            sdk?.pythonInterpreterDirectory?.let {
                addMacroExpand(PY_INTERPRETER_DIRECTORY_MACRO_NAME, it)
            }
        }
        return pathMap.substitute(rawProjectRuffLspExecutablePath, true)
    }
    override fun getState(): RuffConfigService {
        return this
    }

    override fun loadState(config: RuffConfigService) {
        XmlSerializerUtil.copyBean(config, this)
    }


    fun getRuffExecutablePath(sdk: Sdk?): @SystemDependent String? {
        return when {
            alwaysUseGlobalRuff -> globalRuffExecutablePath
            else -> getProjectRuffExecutablePath(sdk) ?: globalRuffExecutablePath
            }
    }
    fun getRuffLspExecutablePath(sdk: Sdk?): @SystemDependent String? {
            return when {
                alwaysUseGlobalRuff -> globalRuffLspExecutablePath
                else -> getProjectRuffLspExecutablePath(sdk) ?: globalRuffLspExecutablePath
            }
        }
    companion object {
        fun getInstance(project: Project): RuffConfigService {
            return project.getService(RuffConfigService::class.java)
        }
    }
}
