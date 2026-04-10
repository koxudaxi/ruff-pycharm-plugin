package com.koxudaxi.ruff

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "RuffApplicationConfigService", storages = [Storage("ruff.application.xml")])
class RuffApplicationConfigService : PersistentStateComponent<RuffApplicationConfigService> {
    var runRuffOnSave: Boolean = false
    var shouldMigrateRunRuffOnSave: Boolean = true

    override fun getState(): RuffApplicationConfigService {
        return this
    }

    override fun loadState(config: RuffApplicationConfigService) {
        XmlSerializerUtil.copyBean(config, this)
    }

    fun migrateRunRuffOnSave(projectConfigService: RuffConfigService) {
        if (!shouldMigrateRunRuffOnSave) {
            return
        }
        if (projectConfigService.runRuffOnSave) {
            runRuffOnSave = true
            projectConfigService.runRuffOnSave = false
        }
    }

    companion object {
        fun getInstance(): RuffApplicationConfigService {
            return service<RuffApplicationConfigService>()
        }
    }
}
