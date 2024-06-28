import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCompletionSupport
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
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
        if (!ruffConfigService.useRuffLsp && !ruffConfigService.useRuffServer) return
        if (!isInspectionEnabled(project)) return
        if (file.extension != "py") return
        val ruffCacheService = RuffCacheService.getInstance(project)
        if (ruffCacheService.getVersion() == null) return
        val descriptor = when {
            ruffConfigService.useRuffServer && ruffCacheService.hasLsp() == true -> {
                getRuffExecutable(project, ruffConfigService , false)?.let { RuffServerServerDescriptor(project, it, ruffConfigService) }
            }
            else -> getRuffExecutable(project, ruffConfigService, true)?.let { RuffLspServerDescriptor(project, it, ruffConfigService) }
        } ?: return
        serverStarter.ensureServerStarted(descriptor)
    }

    private fun getRuffExecutable(project: Project, ruffConfigService: RuffConfigService, lsp: Boolean): File? {
        return when {
            lsp -> ruffConfigService.ruffLspExecutablePath
            else -> ruffConfigService.ruffExecutablePath
        }?.let { File(it) }?.takeIf { it.exists() } ?: detectRuffExecutable(
            project, ruffConfigService, lsp
        )
    }

    private fun isInspectionEnabled(project: Project): Boolean {
        val inspectionProfileManager = ProjectInspectionProfileManager.getInstance(project)

        val toolWrapper = InspectionToolRegistrar.getInstance().createTools()
            .find { it.shortName == RuffInspection.INSPECTION_SHORT_NAME } ?: return false
        return inspectionProfileManager.currentProfile.isToolEnabled(toolWrapper.displayKey)
    }
}

@Suppress("UnstableApiUsage")
private class RuffLspServerDescriptor(project: Project, executable: File, ruffConfig: RuffConfigService) :
    RuffLspServerDescriptorBase(project, executable, ruffConfig) {
    private fun createBaseCommandLine(): GeneralCommandLine = GeneralCommandLine(executable.absolutePath)

    override fun createCommandLine(): GeneralCommandLine {
        val commandLine = createBaseCommandLine()
        if (ruffConfig.useRuffFormat) {
            return commandLine.withEnvironment("RUFF_EXPERIMENTAL_FORMATTER", "1")
        }
        return commandLine
    }
}


@Suppress("UnstableApiUsage")
abstract class RuffLspServerDescriptorBase(project: Project, val executable: File, val ruffConfig: RuffConfigService) :
    ProjectWideLspServerDescriptor(project, "Ruff") {

    override fun isSupportedFile(file: VirtualFile) = file.extension == "py"
    abstract override fun createCommandLine(): GeneralCommandLine
    override fun createInitializationOptions(): Any? {
        val config = ruffConfig.ruffConfigPath?.let { File(it) }?.takeIf { it.exists() } ?: return null
        return InitOptions(Settings(listOf(CONFIG_ARG, config.absolutePath)))
    }

    override val lspGoToDefinitionSupport: Boolean = false
    override val lspCompletionSupport: LspCompletionSupport? = null
}

@Suppress("UnstableApiUsage")
private class RuffServerServerDescriptor(project: Project, executable: File, ruffConfig: RuffConfigService) :
    RuffLspServerDescriptorBase(project, executable, ruffConfig) {
    override fun createCommandLine(): GeneralCommandLine  = GeneralCommandLine(listOf(executable.absolutePath) + LSP_PREVIEW_ARGS)
}
@Serializable
data class Settings(
    val args: List<String>
)


@Serializable
data class InitOptions(
    val settings: Settings
)
