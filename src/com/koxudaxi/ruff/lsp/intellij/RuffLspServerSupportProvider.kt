import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.*
import com.koxudaxi.ruff.*
import com.koxudaxi.ruff.lsp.intellij.supports.*
import com.koxudaxi.ruff.lsp.useCodeActionFeature
import com.koxudaxi.ruff.lsp.useDiagnosticFeature
import com.koxudaxi.ruff.lsp.useFormattingFeature
import com.koxudaxi.ruff.lsp.useHoverFeature
import kotlinx.serialization.Serializable
import org.eclipse.lsp4j.InitializeParams
import java.io.File


@Suppress("UnstableApiUsage")
class RuffLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        val ruffConfigService = project.configService
        if (!ruffConfigService.enableLsp) return
        if (!ruffConfigService.useRuffLsp && !ruffConfigService.useRuffServer) return
        if (!ruffConfigService.useIntellijLspClient) return
        if (!isInspectionEnabled(project)) return
        if (!file.isApplicableTo) return
        val ruffCacheService = RuffCacheService.getInstance(project)
        val descriptor = when {
            ruffConfigService.useRuffServer && ruffCacheService.hasLsp() == true -> {
                getRuffExecutable(project, ruffConfigService, false, ruffCacheService)?.let {
                    RuffServerServerDescriptor(
                        project,
                        it,
                        ruffConfigService
                    )
                }
            }

            else -> getRuffExecutable(project, ruffConfigService, true, ruffCacheService)?.let {
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

internal fun resolveWslLspFileUri(path: String, fallbackUri: String): String =
    buildWslFileUri(path) ?: fallbackUri

internal fun resolveWslLspLocalPath(fileUri: String): String? =
    buildWslUncPath(fileUri)

internal fun normalizeWslInitializeParams(params: InitializeParams): InitializeParams =
    params.apply {
        rootPath = rootPath?.let { toWindowsWslUncPath(it) } ?: rootPath
    }

internal fun formatInitializeParamsLog(params: InitializeParams): String {
    val workspaceFolders = params.workspaceFolders
        ?.joinToString(prefix = "[", postfix = "]") { "{name=${it.name}, uri=${it.uri}}" }
        ?: "[]"
    return "LSP initialize params: rootUri=${params.rootUri}, rootPath=${params.rootPath}, workspaceFolders=$workspaceFolders"
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

    override fun isSupportedFile(file: VirtualFile) = file.isApplicableTo
    abstract override fun createCommandLine(): GeneralCommandLine

    override fun getFileUri(file: VirtualFile): String {
        val fallbackUri = super.getFileUri(file)
        if (!SystemInfo.isWindows) {
            return fallbackUri
        }
        val resolvedUri = resolveWslLspFileUri(file.path, fallbackUri)
        if (resolvedUri != fallbackUri) {
            RuffLoggingService.log(project, "LSP file URI: ${file.path} -> $resolvedUri")
        }
        return resolvedUri
    }

    override fun findFileByUri(fileUri: String): VirtualFile? {
        if (!SystemInfo.isWindows) {
            return super.findFileByUri(fileUri)
        }
        val resolvedPath = resolveWslLspLocalPath(fileUri)
        if (resolvedPath != null) {
            RuffLoggingService.log(project, "LSP resolve URI: $fileUri -> $resolvedPath")
            return findLocalFileByPath(resolvedPath)
        }
        return super.findFileByUri(fileUri)
    }

    override fun createInitializeParams(): InitializeParams {
        val params = super.createInitializeParams()
        if (!SystemInfo.isWindows) {
            return params
        }
        normalizeWslInitializeParams(params)
        RuffLoggingService.log(project, formatInitializeParamsLog(params))
        return params
    }

    override val lspHoverSupport: Boolean = project.useHoverFeature
    override val lspCodeActionsSupport: LspCodeActionsSupport? =
        if (project.useCodeActionFeature) RuffLspCodeActionsSupport(project) else null
    override val lspFormattingSupport: LspFormattingSupport? =
        if (project.useFormattingFeature) RuffLspFormattingSupport(project) else null
    override val lspCommandsSupport: LspCommandsSupport = LspCommandsSupport()
    override val lspDiagnosticsSupport: LspDiagnosticsSupport? =
        if (project.useDiagnosticFeature) RuffLspDiagnosticsSupport(project) else null

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
