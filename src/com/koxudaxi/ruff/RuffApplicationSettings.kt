package com.koxudaxi.ruff

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(
    name = "RuffApplicationSettings",
    storages = [Storage(value = "ruff.machine.xml", roamingType = RoamingType.DISABLED)]
)
class RuffApplicationSettings : PersistentStateComponent<RuffApplicationSettings> {
    var nativeRuffSupportNotificationDismissed: Boolean = false

    override fun getState(): RuffApplicationSettings {
        return this
    }

    override fun loadState(state: RuffApplicationSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): RuffApplicationSettings {
            return ApplicationManager.getApplication().getService(RuffApplicationSettings::class.java)
        }
    }
}
