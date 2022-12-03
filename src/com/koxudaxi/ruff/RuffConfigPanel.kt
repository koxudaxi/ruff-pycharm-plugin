package com.koxudaxi.ruff

import com.intellij.openapi.project.Project

import com.koxudaxi.ruff.RuffConfigService.Companion.getInstance
import javax.swing.JCheckBox
import javax.swing.JPanel

class RuffConfigPanel(project: Project) {
    lateinit var configPanel: JPanel
    private lateinit var runRuffOnSaveCheckBox: JCheckBox

    init {
        val ruffConfigService = getInstance(project)
        runRuffOnSaveCheckBox.isSelected = ruffConfigService.runRuffOnSave
        runRuffOnSaveCheckBox.isEnabled = true
    }
    val runRuffOnSave: Boolean
        get() = runRuffOnSaveCheckBox.isSelected
}