package com.koxudaxi.ruff


import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBTextField
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.emptyText
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
        setProjectRuffExecutablePath(project)
        globalRuffExecutablePathField.apply {
            addBrowseFolderListener(null, null, null, FileChooserDescriptorFactory.createSingleFileDescriptor())
            if (textField is JBTextField) {
                setAutodetectedRuff()
            }
            textField.isEditable = false
        }


        setAutodetectedRuffButton.addActionListener { setAutodetectedRuff() }

        alwaysUseGlobalRuffCheckBox.addActionListener {
            setProjectRuffExecutablePath(project)
        }
    }

    private fun setAutodetectedRuff() =
        when (val ruffExecutable = findGlobalRuffExecutable()) {
            is File -> globalRuffExecutablePathField.text = ruffExecutable.absolutePath
            else -> globalRuffExecutablePathField.emptyText.text = RUFF_EXECUTABLE_NOT_FOUND
        }
    private fun setProjectRuffExecutablePath(project: Project) {
        projectRuffExecutablePathField.text = detectRuffExecutable(project, updateConfig = false, alwaysUseGlobalRuff = alwaysUseGlobalRuffCheckBox.isSelected)?.absolutePath ?: RUFF_EXECUTABLE_NOT_FOUND
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

    companion object {
        const val RUFF_EXECUTABLE_NOT_FOUND =  "Ruff executable not found"
    }
}