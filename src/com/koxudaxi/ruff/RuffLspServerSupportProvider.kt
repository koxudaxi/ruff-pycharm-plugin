import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCompletionSupport
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.jetbrains.python.sdk.pythonSdk
import com.koxudaxi.ruff.*
import kotlinx.serialization.Serializable
import java.io.File



@Suppress("UnstableApiUsage")
class RuffLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        val ruffConfigService = RuffConfigService.getInstance(project)
        if (!ruffConfigService.useRuffLsp) return
        if (!isInspectionEnabled(project)) return
        if (file.extension != "py") return
        val executable =
            ruffConfigService.getRuffLspExecutablePath(project.pythonSdk)?.let { File(it) }?.takeIf { it.exists() } ?: detectRuffExecutable(
                project, ruffConfigService, true
            ) ?: return
        serverStarter.ensureServerStarted(RuffLspServerDescriptor(project, executable, ruffConfigService))
    }

    private fun isInspectionEnabled(project: Project): Boolean {
        val inspectionProfileManager = ProjectInspectionProfileManager.getInstance(project)

        val toolWrapper = InspectionToolRegistrar.getInstance().createTools()
            .find { it.shortName == RuffInspection.INSPECTION_SHORT_NAME } ?: return false
        return inspectionProfileManager.currentProfile.isToolEnabled(toolWrapper.displayKey)
    }
}


@Suppress("UnstableApiUsage")
private class RuffLspServerDescriptor(project: Project, val executable: File, val ruffConfig: RuffConfigService) :
    ProjectWideLspServerDescriptor(project, "Ruff") {

    override fun isSupportedFile(file: VirtualFile) = file.extension == "py"

    private fun createBaseCommandLine(): GeneralCommandLine = GeneralCommandLine(executable.absolutePath)

    override fun createCommandLine(): GeneralCommandLine {
        val commandLine = createBaseCommandLine()
        if (ruffConfig.useRuffFormat) {
            return commandLine.withEnvironment("RUFF_EXPERIMENTAL_FORMATTER", "1")
        }
        return commandLine
    }

    override fun createInitializationOptions(): Any? {
        val config = ruffConfig.ruffConfigPath?.let { File(it) }?.takeIf { it.exists() } ?: return null
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
