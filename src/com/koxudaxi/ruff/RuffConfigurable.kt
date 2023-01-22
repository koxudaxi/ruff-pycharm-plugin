package com.koxudaxi.ruff

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent


class RuffConfigurable internal constructor(project: Project) : Configurable {
    private val ruffConfigService: RuffConfigService = RuffConfigService.getInstance(project)
    private val configPanel: RuffConfigPanel = RuffConfigPanel(project)
    override fun getDisplayName(): String {
        return "Ruff"
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun createComponent(): JComponent {
        reset()
        return configPanel.configPanel
    }

    override fun reset() {}

    override fun isModified(): Boolean {
        return (ruffConfigService.runRuffOnSave != configPanel.runRuffOnSave) ||
                (ruffConfigService.runRuffOnReformatCode != configPanel.runRuffOnReformatCode) ||
                (ruffConfigService.showRuleCode != configPanel.showRuleCode)
    }

    override fun apply() {
        ruffConfigService.runRuffOnSave = configPanel.runRuffOnSave
        ruffConfigService.runRuffOnReformatCode = configPanel.runRuffOnReformatCode
        ruffConfigService.showRuleCode = configPanel.showRuleCode
    }

    override fun disposeUIResources() {
    }
}