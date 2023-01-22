package com.koxudaxi.ruff

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "RuffConfigService", storages = [Storage("ruff.xml")])
class RuffConfigService : PersistentStateComponent<RuffConfigService> {
    var runRuffOnSave: Boolean = false
    var runRuffOnReformatCode: Boolean = true
    var showRuleCode: Boolean = false
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