package com.koxudaxi.ruff


import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBTextField
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.emptyText
import com.jetbrains.python.sdk.pythonSdk
import com.koxudaxi.ruff.RuffConfigService.Companion.getInstance
import org.jetbrains.annotations.SystemDependent
import java.io.File
import javax.swing.*


class RuffConfigPanel(project: Project) {
    lateinit var configPanel: JPanel
    private lateinit var runRuffOnSaveCheckBox: JCheckBox
    private lateinit var runRuffOnReformatCodeCheckBox: JCheckBox
    private lateinit var showRuleCodeCheckBox: JCheckBox
    private lateinit var globalRuffExecutablePathField: TextFieldWithBrowseButton
    private lateinit var projectRuffLabel: JLabel
    private lateinit var globalRuffLspLabel: JLabel
    private lateinit var globalRuffLspExecutablePathField: TextFieldWithBrowseButton
    private lateinit var setAutodetectedRuffButton: JButton
    private lateinit var setAutodetectedRuffLspButton: JButton
    private lateinit var alwaysUseGlobalRuffCheckBox: JCheckBox
    private lateinit var projectRuffExecutablePathField: JBTextField

    private lateinit var projectRuffLspLabel: JLabel
    private lateinit var projectRuffLspExecutablePathField: JBTextField
    private lateinit var ruffConfigPathField: TextFieldWithBrowseButton
    private lateinit var clearRuffConfigPathButton: JButton
    private lateinit var disableOnSaveOutsideOfProjectCheckBox: JCheckBox
    private lateinit var useRuffLspRadioButton: JRadioButton
    private lateinit var useIntellijLspClientRadioButton: JRadioButton
    private lateinit var useLsp4ijRadioButton: JRadioButton
    private lateinit var useRuffFormatCheckBox: JCheckBox
    private lateinit var useRuffServerRadioButton: JRadioButton
    private lateinit var enableLspCheckBox: JCheckBox
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
        useRuffLspRadioButton.isSelected = ruffConfigService.useRuffLsp
        useIntellijLspClientRadioButton.isEnabled = intellijLspClientSupported
        useIntellijLspClientRadioButton.isSelected = ruffConfigService.useIntellijLspClient
        useLsp4ijRadioButton.isSelected = ruffConfigService.useLsp4ij
        useRuffFormatCheckBox.isEnabled = true
        useRuffFormatCheckBox.isSelected = ruffConfigService.useRuffFormat
        useRuffServerRadioButton.isEnabled = true
        useRuffServerRadioButton.isSelected = ruffConfigService.useRuffServer
        disableOnSaveOutsideOfProjectCheckBox.isSelected = ruffConfigService.disableOnSaveOutsideOfProject
        enableLspCheckBox.isSelected = ruffConfigService.enableLsp
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

        globalRuffLspExecutablePathField.apply {
            addBrowseFolderListener(null, null, null, FileChooserDescriptorFactory.createSingleFileDescriptor())
            if (textField is JBTextField) {
                when (val globalRuffLspExecutablePath = ruffConfigService.globalRuffLspExecutablePath?.takeIf { File(it).exists() }) {
                    is String -> textField.text = globalRuffLspExecutablePath
                    else -> setAutodetectedRuffLsp()
                }
            }
            textField.isEditable = false
        }

        updateLspExecutableFields()
        updateProjectExecutableFields()

        setAutodetectedRuffButton.addActionListener {
            setAutodetectedRuff()
        }

        setAutodetectedRuffLspButton.addActionListener { setAutodetectedRuffLsp() }


        useRuffLspRadioButton.addActionListener {
            updateLspClientCheckBoxes()
            updateLspExecutableFields()
        }
        useRuffServerRadioButton.addActionListener {
            updateLspClientCheckBoxes()
            updateLspExecutableFields()
        }

        enableLspCheckBox.addActionListener {
            updateLspFields()
        }
        updateLspFields()
        alwaysUseGlobalRuffCheckBox.addActionListener { updateProjectExecutableFields() }
        when (val projectRuffExecutablePath = ruffConfigService.projectRuffExecutablePath?.takeIf { File(it).exists() } ?: getProjectRuffExecutablePath(project, false)) {
            is String -> projectRuffExecutablePathField.text = projectRuffExecutablePath
            else -> {
                projectRuffExecutablePathField.text = ""
                projectRuffExecutablePathField.emptyText.text = RUFF_EXECUTABLE_NOT_FOUND
            }
        }


        when (val projectRuffLspExecutablePath = ruffConfigService.projectRuffLspExecutablePath?.takeIf { File(it).exists() } ?: getProjectRuffExecutablePath(project, true)) {
            is String -> projectRuffLspExecutablePathField.text = projectRuffLspExecutablePath
            else -> {
                projectRuffLspExecutablePathField.text = ""
                projectRuffLspExecutablePathField.emptyText.text = RUFF_LSP_EXECUTABLE_NOT_FOUND
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
    private fun updateLspClientCheckBoxes() {
        val useLspClient = useRuffLspRadioButton.isSelected || useRuffServerRadioButton.isSelected
        useIntellijLspClientRadioButton.isEnabled = useLspClient
        useLsp4ijRadioButton.isEnabled = useLspClient
    }
    private fun updateLspFields() {
        if (enableLspCheckBox.isSelected) {
            useLsp4ijRadioButton.isEnabled = true
            useIntellijLspClientRadioButton.isEnabled =  intellijLspClientSupported
            useRuffLspRadioButton.isEnabled = true
            useRuffServerRadioButton.isEnabled = true
        } else {
            updateLspClientCheckBoxes()
            updateLspExecutableFields()
            useLsp4ijRadioButton.isEnabled = false
            useIntellijLspClientRadioButton.isEnabled = false
            useRuffLspRadioButton.isEnabled = false
            useRuffServerRadioButton.isEnabled = false
        }
    }
    private fun updateLspExecutableFields() {
        val enabled = intellijLspClientSupported && useRuffLspRadioButton.isSelected
        globalRuffLspExecutablePathField.isEnabled = enabled
        globalRuffLspLabel.isEnabled = enabled
        setAutodetectedRuffLspButton.isEnabled = enabled
        if (!alwaysUseGlobalRuffCheckBox.isSelected) {
            projectRuffLspExecutablePathField.isEnabled = enabled
            projectRuffLspLabel.isEnabled = enabled
        }
    }
    private fun updateProjectExecutableFields() {
        val enabled = !alwaysUseGlobalRuffCheckBox.isSelected
        projectRuffExecutablePathField.isEnabled = enabled
        projectRuffLabel.isEnabled = enabled
        if (intellijLspClientSupported && useRuffLspRadioButton.isSelected) {
            projectRuffLspExecutablePathField.isEnabled = enabled
            projectRuffLspLabel.isEnabled = enabled
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

    private fun setAutodetectedRuffLsp() =
        when (val ruffLspExecutablePath = findGlobalRuffExecutable(true)?.absolutePath) {
            is String -> globalRuffLspExecutablePathField.text = ruffLspExecutablePath
            else -> {
                globalRuffLspExecutablePathField.text = ""
                globalRuffLspExecutablePathField.emptyText.text = RUFF_LSP_EXECUTABLE_NOT_FOUND
            }
        }

    private fun getProjectRuffExecutablePath(project: Project, lsp: Boolean): String? {
     return project.pythonSdk?.let { findRuffExecutableInSDK(it, lsp) }?.absolutePath
    }
    val runRuffOnSave: Boolean
        get() = runRuffOnSaveCheckBox.isSelected
    val runRuffOnReformatCode: Boolean
        get() = runRuffOnReformatCodeCheckBox.isSelected
    val showRuleCode: Boolean
        get() = showRuleCodeCheckBox.isSelected
    val globalRuffExecutablePath: @SystemDependent String?
        get() = globalRuffExecutablePathField.text.takeIf { it.isNotBlank() }

    val globalRuffLspExecutablePath: @SystemDependent String?
        get() = globalRuffLspExecutablePathField.text.takeIf { it.isNotBlank() }
    val alwaysUseGlobalRuff: Boolean
        get() = alwaysUseGlobalRuffCheckBox.isSelected
    val ruffConfigPath: @SystemDependent String?
        get() = ruffConfigPathField.text.takeIf { it.isNotBlank() }
    val disableOnSaveOutsideOfProject: Boolean
        get() = disableOnSaveOutsideOfProjectCheckBox.isSelected
    val useRuffLsp: Boolean
        get() = useRuffLspRadioButton.isSelected
    val useIntellijLspClient: Boolean
        get() = useIntellijLspClientRadioButton.isSelected
    val useLsp4ij: Boolean
        get() = useLsp4ijRadioButton.isSelected
    val useRuffServer: Boolean
        get() = useRuffServerRadioButton.isSelected
    val useRuffFormat: Boolean
        get() = useRuffFormatCheckBox.isSelected
    val enableLsp: Boolean
        get() = enableLspCheckBox.isSelected

    companion object {
        const val RUFF_EXECUTABLE_NOT_FOUND =  "Ruff executable not found"
        const val RUFF_LSP_EXECUTABLE_NOT_FOUND =  "Ruff-lsp executable not found"
    }
}