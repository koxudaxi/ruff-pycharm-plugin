package com.koxudaxi.ruff

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.emptyText
import com.intellij.ui.components.JBTextField
import com.jetbrains.python.sdk.pythonSdk
import com.koxudaxi.ruff.RuffConfigService.Companion.getInstance
import org.jetbrains.annotations.SystemDependent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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
    private lateinit var lsp4ijLinkLabel: JLabel
    private lateinit var enableRuffLoggingCheckBox: JCheckBox
    private lateinit var codeActionFeatureCheckBox: JCheckBox
    private lateinit var formattingFeatureCheckBox: JCheckBox
    private lateinit var diagnosticFeatureCheckBox: JCheckBox
    private lateinit var hoverFeatureCheckBox: JCheckBox
    private lateinit var useRuffImportOptimizerCheckBox: JCheckBox



    init {
        val ruffConfigService = getInstance(project)
        val ruffCacheService = RuffCacheService.getInstance(project)
        runRuffOnSaveCheckBox.isEnabled = true
        runRuffOnReformatCodeCheckBox.isEnabled = true
        showRuleCodeCheckBox.isEnabled = true
        alwaysUseGlobalRuffCheckBox.isEnabled = true
        useRuffLspRadioButton.isEnabled = lspSupported
        useIntellijLspClientRadioButton.isEnabled = intellijLspClientSupported
        useRuffFormatCheckBox.isEnabled = true
        useRuffServerRadioButton.isEnabled = lspSupported
        enableLspCheckBox.isEnabled = lspSupported
        useRuffImportOptimizerCheckBox.isEnabled = true


        lsp4ijLinkLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                BrowserUtil.browse("https://plugins.jetbrains.com/plugin/23257-lsp4ij")
            }
        })
        runRuffOnSaveCheckBox.addActionListener {
            disableOnSaveOutsideOfProjectCheckBox.isEnabled = runRuffOnSaveCheckBox.isSelected
        }

        globalRuffExecutablePathField.apply {
            addBrowseFolderListener(null, FileChooserDescriptorFactory.createSingleFileDescriptor())
            textField.isEditable = false
        }

        globalRuffLspExecutablePathField.apply {
            addBrowseFolderListener(null, FileChooserDescriptorFactory.createSingleFileDescriptor())
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
        alwaysUseGlobalRuffCheckBox.addActionListener { updateProjectExecutableFields() }
        when (val projectRuffExecutablePath =
            ruffCacheService.getProjectRuffExecutablePath()?.takeIf { File(it).exists() }
                ?: getProjectRuffExecutablePath(project, false)) {
            is String -> projectRuffExecutablePathField.text = projectRuffExecutablePath
            else -> {
                projectRuffExecutablePathField.text = ""
                projectRuffExecutablePathField.emptyText.text = RUFF_EXECUTABLE_NOT_FOUND
            }
        }


        when (val projectRuffLspExecutablePath =
            ruffCacheService.getProjectRuffLspExecutablePath()?.takeIf { File(it).exists() }
                ?: getProjectRuffExecutablePath(project, true)) {
            is String -> projectRuffLspExecutablePathField.text = projectRuffLspExecutablePath
            else -> {
                projectRuffLspExecutablePathField.text = ""
                projectRuffLspExecutablePathField.emptyText.text = RUFF_LSP_EXECUTABLE_NOT_FOUND
            }
        }

        ruffConfigPathField.apply {
            addBrowseFolderListener(null, FileChooserDescriptorFactory.createSingleFileDescriptor())
            if (textField is JBTextField) {
                (textField as JBTextField).emptyText.text = "Leave blank to auto-discover, or enter a path to override."
            }
            textField.isEditable = true
            textField.text = ruffConfigService.ruffConfigPath
        }
        clearRuffConfigPathButton.addActionListener {
            ruffConfigPathField.text = ""
        }
        reset(ruffConfigService)
    }

    fun reset(ruffConfigService: RuffConfigService) {
        runRuffOnSaveCheckBox.isSelected = ruffConfigService.runRuffOnSave
        runRuffOnReformatCodeCheckBox.isSelected = ruffConfigService.runRuffOnReformatCode
        showRuleCodeCheckBox.isSelected = ruffConfigService.showRuleCode
        alwaysUseGlobalRuffCheckBox.isSelected = ruffConfigService.alwaysUseGlobalRuff
        disableOnSaveOutsideOfProjectCheckBox.isSelected = ruffConfigService.disableOnSaveOutsideOfProject
        disableOnSaveOutsideOfProjectCheckBox.isEnabled = ruffConfigService.runRuffOnSave
        useRuffLspRadioButton.isSelected = ruffConfigService.useRuffLsp
        useIntellijLspClientRadioButton.isSelected = ruffConfigService.useIntellijLspClient
        useLsp4ijRadioButton.isSelected = ruffConfigService.useLsp4ij
        useRuffFormatCheckBox.isSelected = ruffConfigService.useRuffFormat
        useRuffServerRadioButton.isSelected = ruffConfigService.useRuffServer || !ruffConfigService.useRuffLsp
        enableLspCheckBox.isSelected = ruffConfigService.enableLsp
        enableRuffLoggingCheckBox.isSelected = ruffConfigService.enableRuffLogging
        codeActionFeatureCheckBox.isSelected = ruffConfigService.codeActionFeature
        formattingFeatureCheckBox.isSelected = ruffConfigService.formattingFeature
        diagnosticFeatureCheckBox.isSelected = ruffConfigService.diagnosticFeature
        hoverFeatureCheckBox.isSelected = ruffConfigService.hoverFeature
        useRuffImportOptimizerCheckBox.isSelected = ruffConfigService.useRuffImportOptimizer
        updateExecutableField(
            globalRuffExecutablePathField,
            ruffConfigService.globalRuffExecutablePath,
            findGlobalRuffExecutable(false)?.absolutePath,
            RUFF_EXECUTABLE_NOT_FOUND
        )
        updateExecutableField(
            globalRuffLspExecutablePathField,
            ruffConfigService.globalRuffLspExecutablePath,
            findGlobalRuffExecutable(true)?.absolutePath,
            RUFF_LSP_EXECUTABLE_NOT_FOUND
        )
        ruffConfigPathField.text = ruffConfigService.ruffConfigPath ?: ""
        updateLspFields()
        updateLspExecutableFields()
        updateProjectExecutableFields()
    }


    private fun updateLspClientCheckBoxes() {
        val useLspClient = useRuffLspRadioButton.isSelected || useRuffServerRadioButton.isSelected
        useIntellijLspClientRadioButton.isEnabled = useLspClient && intellijLspClientSupported
        useLsp4ijRadioButton.isEnabled = useLspClient && lsp4ijSupported
    }

    private fun updateLspFields() {
        if (enableLspCheckBox.isSelected) {
            useLsp4ijRadioButton.isEnabled = lsp4ijSupported
            useIntellijLspClientRadioButton.isEnabled = intellijLspClientSupported
            useRuffLspRadioButton.isEnabled = lspSupported
            useRuffServerRadioButton.isEnabled = lspSupported
            hoverFeatureCheckBox.isEnabled = true
            codeActionFeatureCheckBox.isEnabled = true
            formattingFeatureCheckBox.isEnabled = true
            diagnosticFeatureCheckBox.isEnabled = true
        } else {
            updateLspClientCheckBoxes()
            updateLspExecutableFields()
            useLsp4ijRadioButton.isEnabled = false
            useIntellijLspClientRadioButton.isEnabled = false
            useRuffLspRadioButton.isEnabled = false
            useRuffServerRadioButton.isEnabled = false
            hoverFeatureCheckBox.isEnabled = false
            codeActionFeatureCheckBox.isEnabled = false
            formattingFeatureCheckBox.isEnabled = false
            diagnosticFeatureCheckBox.isEnabled = false
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

    private fun updateExecutableField(
        field: TextFieldWithBrowseButton,
        configuredPath: String?,
        autodetectedPath: String?,
        notFoundText: String
    ) {
        field.text = ""
        val textField = field.textField
        if (textField is JBTextField) {
            when (val existingPath = configuredPath?.takeIf { File(it).exists() }) {
                is String -> textField.text = existingPath
                else -> textField.emptyText.text = autodetectedPath ?: notFoundText
            }
        }
    }

    private fun getProjectRuffExecutablePath(project: Project, lsp: Boolean): String? {
        return project.preferredPythonSdk?.let { findRuffExecutableInSDK(it, lsp) }?.absolutePath
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
    val enableRuffLogging: Boolean
        get() = enableRuffLoggingCheckBox.isSelected
    val codeActionFeature: Boolean
        get() = codeActionFeatureCheckBox.isSelected
    val formattingFeature: Boolean
        get() = formattingFeatureCheckBox.isSelected
    val diagnosticFeature: Boolean
        get() = diagnosticFeatureCheckBox.isSelected
    val hoverFeature: Boolean
        get() = hoverFeatureCheckBox.isSelected
    val useRuffImportOptimizer: Boolean
        get() = useRuffImportOptimizerCheckBox.isSelected
    companion object {
        const val RUFF_EXECUTABLE_NOT_FOUND = "Ruff executable not found"
        const val RUFF_LSP_EXECUTABLE_NOT_FOUND = "Ruff-lsp executable not found"
    }
}
