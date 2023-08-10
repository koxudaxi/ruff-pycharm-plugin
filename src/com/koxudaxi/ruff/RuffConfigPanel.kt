package com.koxudaxi.ruff


import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBTextField
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.emptyText
import com.jetbrains.python.sdk.pythonSdk
import com.koxudaxi.ruff.RuffConfigService.Companion.getInstance
import org.jetbrains.annotations.SystemDependent
import java.io.File

import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JPanel


class RuffConfigPanel(project: Project) {
    lateinit var configPanel: JPanel
    private lateinit var runRuffOnSaveCheckBox: JCheckBox
    private lateinit var runRuffOnReformatCodeCheckBox: JCheckBox
    private lateinit var showRuleCodeCheckBox: JCheckBox
    private lateinit var globalRuffExecutablePathField: TextFieldWithBrowseButton
    private lateinit var setAutodetectedRuffButton: JButton
    private lateinit var alwaysUseGlobalRuffCheckBox: JCheckBox
    private lateinit var projectRuffExecutablePathField: JBTextField
    private lateinit var ruffConfigPathField: TextFieldWithBrowseButton
    private lateinit var clearRuffConfigPathButton: JButton
    private lateinit var disableOnSaveOutsideOfProjectCheckBox: JCheckBox
    private lateinit var useRuffLspCheckBox: JCheckBox
    init {
        val ruffConfigService = getInstance(project)
        runRuffOnSaveCheckBox.isSelected = ruffConfigService.runRuffOnSave
        runRuffOnSaveCheckBox.isEnabled = true
        runRuffOnReformatCodeCheckBox.isSelected = ruffConfigService.runRuffOnReformatCode
        runRuffOnReformatCodeCheckBox.isEnabled = true
        showRuleCodeCheckBox.isSelected = ruffConfigService.showRuleCode
        showRuleCodeCheckBox.isEnabled = true
        alwaysUseGlobalRuffCheckBox.isEnabled = true
        alwaysUseGlobalRuffCheckBox.isSelected = ruffConfigService.alwaysUseGlobalRuff
        disableOnSaveOutsideOfProjectCheckBox.isEnabled = ruffConfigService.runRuffOnSave
        useRuffLspCheckBox.isSelected = ruffConfigService.useRuffLsp
        disableOnSaveOutsideOfProjectCheckBox.isSelected = ruffConfigService.disableOnSaveOutsideOfProject
        runRuffOnSaveCheckBox.addActionListener {
            disableOnSaveOutsideOfProjectCheckBox.isEnabled = runRuffOnSaveCheckBox.isSelected
        }

        globalRuffExecutablePathField.apply {
            addBrowseFolderListener(null, null, null, FileChooserDescriptorFactory.createSingleFileDescriptor())
            if (textField is JBTextField) {
                when (val globalRuffExecutablePath = ruffConfigService.globalRuffExecutablePath?.takeIf { File(it).exists() }) {
                    is String -> textField.text = globalRuffExecutablePath
                    else -> setAutodetectedRuff()
                }
            }
            textField.isEditable = false
        }


        setAutodetectedRuffButton.addActionListener { setAutodetectedRuff() }

        alwaysUseGlobalRuffCheckBox.addActionListener {
            projectRuffExecutablePathField.isEnabled = !alwaysUseGlobalRuffCheckBox.isSelected
        }

        when (val projectRuffExecutablePath = ruffConfigService.projectRuffExecutablePath?.takeIf { File(it).exists() } ?: getProjectRuffExecutablePath(project)) {
            is String -> projectRuffExecutablePathField.text = projectRuffExecutablePath
            else -> {
                projectRuffExecutablePathField.text = ""
                projectRuffExecutablePathField.emptyText.text = RUFF_EXECUTABLE_NOT_FOUND
            }
        }
        ruffConfigPathField.apply {
            addBrowseFolderListener(null, null, null, FileChooserDescriptorFactory.createSingleFileDescriptor())
            textField.isEditable = false
            textField.text = ruffConfigService.ruffConfigPath
        }
        clearRuffConfigPathButton.addActionListener {
            ruffConfigPathField.text = ""
        }
    }

    private fun setAutodetectedRuff() =
        when (val ruffExecutablePath = findGlobalRuffExecutable(false)?.absolutePath) {
            is String -> globalRuffExecutablePathField.text = ruffExecutablePath
            else -> {
                globalRuffExecutablePathField.text = ""
                globalRuffExecutablePathField.emptyText.text = RUFF_EXECUTABLE_NOT_FOUND
            }
        }
    private fun getProjectRuffExecutablePath(project: Project): String? {
     return project.pythonSdk?.let { findRuffExecutableInSDK(it, false) }?.absolutePath
    }
    val runRuffOnSave: Boolean
        get() = runRuffOnSaveCheckBox.isSelected
    val runRuffOnReformatCode: Boolean
        get() = runRuffOnReformatCodeCheckBox.isSelected
    val showRuleCode: Boolean
        get() = showRuleCodeCheckBox.isSelected
    val globalRuffExecutablePath: @SystemDependent String?
        get() = globalRuffExecutablePathField.text.takeIf { it.isNotBlank() }
    val alwaysUseGlobalRuff: Boolean
        get() = alwaysUseGlobalRuffCheckBox.isSelected
    val ruffConfigPath: @SystemDependent String?
        get() = ruffConfigPathField.text.takeIf { it.isNotBlank() }
    val disableOnSaveOutsideOfProject: Boolean
        get() = disableOnSaveOutsideOfProjectCheckBox.isSelected
    val useRuffLsp: Boolean
        get() = useRuffLspCheckBox.isSelected
    companion object {
        const val RUFF_EXECUTABLE_NOT_FOUND =  "Ruff executable not found"
    }
}