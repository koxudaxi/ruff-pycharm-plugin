import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.Lsp4jClient
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.LspServerListener
import com.intellij.platform.lsp.api.LspServerNotificationsHandler
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.*
import com.koxudaxi.ruff.*
import com.koxudaxi.ruff.lsp.intellij.supports.*
import com.koxudaxi.ruff.lsp.useCodeActionFeature
import com.koxudaxi.ruff.lsp.useDiagnosticFeature
import com.koxudaxi.ruff.lsp.useFormattingFeature
import com.koxudaxi.ruff.lsp.useHoverFeature
import kotlinx.serialization.Serializable
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
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
        RuffLoggingService.log(
            project,
            "Ensuring IntelliJ LSP server: mode=${descriptor.logName}, executable=${descriptor.executable.path}, diagnostics=${project.useDiagnosticFeature}, formatting=${project.useFormattingFeature}, hover=${project.useHoverFeature}, codeActions=${project.useCodeActionFeature}"
        )
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

internal fun formatLspCommandLineLog(name: String, commandLine: GeneralCommandLine): String =
    "$name command: ${commandLine.commandLineString}"

internal fun formatInitializeResultLog(result: InitializeResult): String {
    val capabilities = result.capabilities
    val diagnosticProvider = capabilities.diagnosticProvider != null
    val syncKind = capabilities.textDocumentSync?.left ?: capabilities.textDocumentSync?.right?.change
    val hoverProvider = capabilities.hoverProvider?.left ?: capabilities.hoverProvider
    val codeActionProvider = capabilities.codeActionProvider?.left ?: capabilities.codeActionProvider
    val documentFormattingProvider =
        capabilities.documentFormattingProvider?.left ?: capabilities.documentFormattingProvider
    val documentRangeFormattingProvider =
        capabilities.documentRangeFormattingProvider?.left ?: capabilities.documentRangeFormattingProvider
    return "LSP initialize result: diagnosticProvider=$diagnosticProvider, textDocumentSync=$syncKind, hoverProvider=$hoverProvider, codeActionProvider=$codeActionProvider, documentFormattingProvider=$documentFormattingProvider, documentRangeFormattingProvider=$documentRangeFormattingProvider"
}

internal fun formatPublishDiagnosticsLog(params: PublishDiagnosticsParams): String =
    "LSP publish diagnostics: uri=${params.uri}, count=${params.diagnostics?.size ?: 0}, version=${params.version}"

internal fun formatServerMessageLog(prefix: String, params: MessageParams): String =
    "$prefix: type=${params.type}, message=${params.message}"

private fun shouldLogWindowsLspDetails(): Boolean = SystemInfo.isWindows

@Suppress("UnstableApiUsage")
private class RuffLspServerDescriptor(project: Project, executable: File, ruffConfig: RuffConfigService) :
    RuffLspServerDescriptorBase(project, executable, ruffConfig) {
    override val logName: String = "IntelliJ Ruff LSP"
    private fun createBaseCommandLine(): GeneralCommandLine? =
        getGeneralCommandLine(executable, project)

    override fun createCommandLine(): GeneralCommandLine {
        val commandLine = createBaseCommandLine() ?: return GeneralCommandLine()
        if (ruffConfig.useRuffFormat) {
            val formattedCommandLine = commandLine.withEnvironment("RUFF_EXPERIMENTAL_FORMATTER", "1")
            RuffLoggingService.log(project, formatLspCommandLineLog(logName, formattedCommandLine))
            return formattedCommandLine
        }
        RuffLoggingService.log(project, formatLspCommandLineLog(logName, commandLine))
        return commandLine
    }
}


@Suppress("UnstableApiUsage")
abstract class RuffLspServerDescriptorBase(project: Project, val executable: File, val ruffConfig: RuffConfigService) :
    ProjectWideLspServerDescriptor(project, "Ruff") {
    abstract val logName: String

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

    override fun createLsp4jClient(serverNotificationsHandler: LspServerNotificationsHandler): Lsp4jClient {
        if (!SystemInfo.isWindows) {
            return super.createLsp4jClient(serverNotificationsHandler)
        }
        return Lsp4jClient(
            LoggingLspServerNotificationsHandler(
                project,
                serverNotificationsHandler
            )
        )
    }

    override val lspServerListener: LspServerListener
        get() {
            if (!SystemInfo.isWindows) {
                return super.lspServerListener ?: object : LspServerListener {}
            }
            return object : LspServerListener {
            override fun serverInitialized(params: InitializeResult) {
                if (shouldLogWindowsLspDetails()) {
                    RuffLoggingService.log(project, formatInitializeResultLog(params))
                }
            }

            override fun serverStopped(unexpected: Boolean) {
                if (shouldLogWindowsLspDetails()) {
                    RuffLoggingService.log(project, "LSP server stopped: unexpected=$unexpected")
                }
            }
        }
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
    override val logName: String = "IntelliJ Ruff server"

    override fun createCommandLine(): GeneralCommandLine {
        val args = project.LSP_ARGS + (getConfigArgs(ruffConfig) ?: listOf())
        val commandLine = getGeneralCommandLine(executable, project, *args.toTypedArray()) ?: return GeneralCommandLine()
        RuffLoggingService.log(project, formatLspCommandLineLog(logName, commandLine))
        return commandLine
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

private class LoggingLspServerNotificationsHandler(
    private val project: Project,
    private val delegate: LspServerNotificationsHandler
) : LspServerNotificationsHandler by delegate {
    override fun publishDiagnostics(params: PublishDiagnosticsParams) {
        if (shouldLogWindowsLspDetails()) {
            RuffLoggingService.log(project, formatPublishDiagnosticsLog(params))
        }
        delegate.publishDiagnostics(params)
    }

    override fun logMessage(params: MessageParams) {
        if (shouldLogWindowsLspDetails()) {
            RuffLoggingService.log(project, formatServerMessageLog("LSP log message", params))
        }
        delegate.logMessage(params)
    }

    override fun showMessage(params: MessageParams) {
        if (shouldLogWindowsLspDetails()) {
            RuffLoggingService.log(project, formatServerMessageLog("LSP show message", params))
        }
        delegate.showMessage(params)
    }

    override fun refreshDiagnostics() =
        delegate.refreshDiagnostics().also {
            if (shouldLogWindowsLspDetails()) {
                RuffLoggingService.log(project, "LSP requested diagnostic refresh")
            }
        }
}
