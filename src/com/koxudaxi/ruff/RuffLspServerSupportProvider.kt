import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCompletionSupport
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.koxudaxi.ruff.*
import kotlinx.serialization.Serializable
import java.io.File


class RuffLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        val ruffConfigService = RuffConfigService.getInstance(project)
        if (!ruffConfigService.useRuffLsp) return
        if (!isInspectionEnabled(project)) return
        if (file.extension != "py") return
        val executable =
            ruffConfigService.ruffLspExecutablePath?.let { File(it) }?.takeIf { it.exists() } ?: detectRuffExecutable(
                project, ruffConfigService, true
            ) ?: return
        val config = ruffConfigService.ruffConfigPath?.let { File(it) }?.takeIf { it.exists() }
        serverStarter.ensureServerStarted(RuffLspServerDescriptor(project, executable, config))
    }

    fun isInspectionEnabled(project: Project): Boolean {
        val inspectionProfileManager = ProjectInspectionProfileManager.getInstance(project)

        val toolWrapper = InspectionToolRegistrar.getInstance().createTools()
            .find { it.shortName == RuffInspection.INSPECTION_SHORT_NAME } ?: return false
        return inspectionProfileManager.currentProfile.isToolEnabled(toolWrapper.displayKey)
    }
}


private class RuffLspServerDescriptor(project: Project, val executable: File, val config: File?) : ProjectWideLspServerDescriptor(project, "Ruff") {
    override fun isSupportedFile(file: VirtualFile) = file.extension == "py"
    override fun createCommandLine(): GeneralCommandLine = GeneralCommandLine(executable.absolutePath)
    override fun createInitializationOptions(): Any? {
        if (config == null) return null
        return InitOptions(Settings(listOf("--config", config.absolutePath)))
    }

    override val lspGoToDefinitionSupport: Boolean = false
    override val lspCompletionSupport: LspCompletionSupport? = null
}

@Serializable
data class Settings(
    val args: List<String>
)


@Serializable
data class InitOptions(
    val settings: Settings
)
