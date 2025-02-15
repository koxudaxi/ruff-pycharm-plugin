package com.koxudaxi.ruff

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.SystemDependent

@Service(Service.Level.PROJECT)
@State(name = "RuffConfigService", storages = [Storage("ruff.xml")])
class RuffConfigService : PersistentStateComponent<RuffConfigService> {
    var runRuffOnSave: Boolean = false
    var runRuffOnReformatCode: Boolean = true
    var showRuleCode: Boolean = true
    var globalRuffExecutablePath: @SystemDependent String? = null
    var globalRuffLspExecutablePath: @SystemDependent String? = null
    var alwaysUseGlobalRuff: Boolean = false
    var ruffConfigPath: @SystemDependent String? = null
    var disableOnSaveOutsideOfProject: Boolean = true
    var useRuffLsp: Boolean = false
    var useIntellijLspClient: Boolean = intellijLspClientSupported
    var useLsp4ij: Boolean = !useIntellijLspClient
    var useRuffServer: Boolean = false
    var useRuffFormat: Boolean = false
    var enableLsp: Boolean = true
    var enableRuffLogging: Boolean = false

    override fun getState(): RuffConfigService {
        return this
    }

    override fun loadState(config: RuffConfigService) {
        XmlSerializerUtil.copyBean(config, this)
    }
    companion object {
        fun getInstance(project: Project): RuffConfigService {
            return project.getService(RuffConfigService::class.java)
        }
    }

}