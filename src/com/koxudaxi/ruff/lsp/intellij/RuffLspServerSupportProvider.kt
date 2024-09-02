import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCompletionSupport
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
        if (!ruffConfigService.enableLsp) return
        if (!ruffConfigService.useRuffLsp && !ruffConfigService.useRuffServer) return
        if (!ruffConfigService.useIntellijLspClient) return
        if (!isInspectionEnabled(project)) return
        if (file.extension != "py") return
        val ruffCacheService = RuffCacheService.getInstance(project)
        if (ruffCacheService.getVersion() == null) return
        val descriptor = when {
            ruffConfigService.useRuffServer && ruffCacheService.hasLsp() == true -> {
                getRuffExecutable(project, ruffConfigService, false)?.let {
                    RuffServerServerDescriptor(
                        project,
                        it,
                        ruffConfigService
                    )
                }
            }

            else -> getRuffExecutable(project, ruffConfigService, true)?.let {
                RuffLspServerDescriptor(
                    project,
                    it,
                    ruffConfigService
                )
            }
        } ?: return
        serverStarter.ensureServerStarted(descriptor)
    }
}

@Suppress("UnstableApiUsage")
private class RuffLspServerDescriptor(project: Project, executable: File, ruffConfig: RuffConfigService) :
    RuffLspServerDescriptorBase(project, executable, ruffConfig) {
    private fun createBaseCommandLine(): GeneralCommandLine? =
        getGeneralCommandLine(executable, project)

    override fun createCommandLine(): GeneralCommandLine {
        val commandLine = createBaseCommandLine() ?: return GeneralCommandLine()
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


    override val lspGoToDefinitionSupport: Boolean = false
    override val lspCompletionSupport: LspCompletionSupport? = null
}

@Suppress("UnstableApiUsage")
private class RuffServerServerDescriptor(project: Project, executable: File, ruffConfig: RuffConfigService) :
    RuffLspServerDescriptorBase(project, executable, ruffConfig) {

    override fun createCommandLine(): GeneralCommandLine {
        val args = project.LSP_ARGS + (getConfigArgs(ruffConfig) ?: listOf())
        return getGeneralCommandLine(executable, project, *args.toTypedArray()) ?: GeneralCommandLine()
    }
}
@Serializable
data class Settings(
    val args: List<String>
)


@Serializable
data class InitOptions(
    val settings: Settings
)
