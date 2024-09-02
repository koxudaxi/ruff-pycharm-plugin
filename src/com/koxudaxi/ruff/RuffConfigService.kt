package com.koxudaxi.ruff

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.SystemDependent

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
    var useIntellijLspClient: Boolean = intellijLspClientSupported
    var useLsp4ij: Boolean = !useIntellijLspClient
    var useRuffServer: Boolean = false
    var useRuffFormat: Boolean = false
    var enableLsp: Boolean = true

    override fun getState(): RuffConfigService {
        return this
    }

    override fun loadState(config: RuffConfigService) {
        XmlSerializerUtil.copyBean(config, this)
    }
    val ruffExecutablePath: @SystemDependent String?
        get() {
        return when {
            alwaysUseGlobalRuff -> globalRuffExecutablePath
            else -> projectRuffExecutablePath ?: globalRuffExecutablePath
        }
    }
    val ruffLspExecutablePath: @SystemDependent String?
        get() {
            return when {
                alwaysUseGlobalRuff -> globalRuffLspExecutablePath
                else -> projectRuffLspExecutablePath ?: globalRuffLspExecutablePath
            }
        }
    companion object {
        fun getInstance(project: Project): RuffConfigService {
            return project.getService(RuffConfigService::class.java)
        }
    }

}