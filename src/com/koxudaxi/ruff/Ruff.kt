package com.koxudaxi.ruff

import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.credentialStore.toByteArrayAndClear
import com.intellij.execution.ExecutionException
import com.intellij.execution.RunCanceledByUserException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.wsl.WSLCommandLineOptions
import com.intellij.execution.wsl.WslPath
import com.intellij.execution.wsl.target.WslTargetEnvironmentConfiguration
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.jetbrains.python.PythonLanguage
//import com.jetbrains.python.execution.FailureReason
import com.jetbrains.python.packaging.IndicatedProcessOutputListener
import com.jetbrains.python.packaging.PyCondaPackageService
import com.jetbrains.python.packaging.PyExecutionException
import com.jetbrains.python.sdk.*
import com.jetbrains.python.sdk.flavors.conda.CondaEnvSdkFlavor
import com.jetbrains.python.target.PyTargetAwareAdditionalData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOError
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.regex.Pattern
import java.nio.file.Paths
import kotlin.io.path.pathString

val RUFF_COMMAND = when {
    SystemInfo.isWindows -> "ruff.exe"
    else -> "ruff"
}
const val WSL_RUFF_COMMAND = "ruff"

val RUFF_LSP_COMMAND = when {
    SystemInfo.isWindows -> "ruff-lsp.exe"
    else -> "ruff-lsp"
}
const val WSL_RUFF_LSP_COMMAND = "ruff-lsp"

const val CONFIG_ARG = "--config"

val SUPPORTED_FILE_EXTENSIONS = listOf("py", "pyi")

val ruffVersionCache: ConcurrentHashMap<String, RuffVersion> = ConcurrentHashMap()

fun getRuffCommand(lsp: Boolean) = if (lsp) RUFF_LSP_COMMAND else RUFF_COMMAND

fun getRuffWlsCommand(lsp: Boolean) = if (lsp) WSL_RUFF_LSP_COMMAND else WSL_RUFF_COMMAND


val SCRIPT_DIR = when {
    SystemInfo.isWindows -> "Scripts"
    else -> "bin"
}

fun getUserSiteRuffPath(lsp: Boolean) =
    PythonSdkUtil.getUserSite() + File.separator + "bin" + File.separator + getRuffCommand(lsp)


val json = Json { ignoreUnknownKeys = true }

val ARGS_BASE = listOf("--exit-zero", "--no-cache", "--force-exclude")
val FIX_ARGS_BASE = ARGS_BASE + listOf("--fix")
val CHECK = listOf("check")
val NO_FIX_FORMAT_ARGS = ARGS_BASE + listOf("--no-fix", "--format", "json")
val NO_FIX_OUTPUT_FORMAT_ARGS = ARGS_BASE + listOf("--no-fix", "--output-format", "json")
val FORMAT_ARGS = listOf("format", "--force-exclude", "--quiet")
val FORMAT_CHECK_ARGS = FORMAT_ARGS + listOf("--check")
val FORMAT_RANGE_ARGS = listOf("--range")
val SELECT_ARGS = listOf("--select")
val IMPORT_SORT_RULES = listOf("I")
val REMOVE_UNUSED_IMPORTS_RULES = listOf("F401")
val OPTIMIZE_IMPORTS_RULES = SELECT_ARGS + listOf((IMPORT_SORT_RULES + REMOVE_UNUSED_IMPORTS_RULES).joinToString(","))
val LSP_ARGS_BASE = listOf("server")
val PREVIEW_ARGS = listOf("--preview")
val Project.OPTIMIZE_IMPORTS_ARGS: List<String>
    get() = when {
        RuffCacheService.hasCheck(this) == true -> CHECK + FIX_ARGS_BASE + OPTIMIZE_IMPORTS_RULES
        else -> FIX_ARGS_BASE + OPTIMIZE_IMPORTS_RULES
    }
val Project.LSP_ARGS: List<String>
    get() = when {
        RuffCacheService.hasStableServer(this) == true -> LSP_ARGS_BASE
        else -> LSP_ARGS_BASE + PREVIEW_ARGS
    }
val Project.RULE_ARGS: List<String>
    get() = when {
        RuffCacheService.hasCheck(this) == true -> listOf("rule")
        else -> listOf("--explain")
    }

val Project.FIX_ARGS: List<String>
    get() = when {
        RuffCacheService.hasCheck(this) == true -> CHECK + FIX_ARGS_BASE
        else -> FIX_ARGS_BASE
    }

val Project.NO_FIX_ARGS: List<String>?
    get() = when (RuffCacheService.hasOutputFormat(this)) {
        true -> NO_FIX_OUTPUT_FORMAT_ARGS
        false -> NO_FIX_FORMAT_ARGS
        else -> null
    }?.let {
        when (RuffCacheService.hasCheck(this)) {
            true -> CHECK + it
            false -> it
            else -> it
        }
    }
private var wslSdkIsSupported: Boolean? = null
val Sdk.wslIsSupported: Boolean
    get() {
        if (wslSdkIsSupported is Boolean) {
            return wslSdkIsSupported as Boolean
        }
        return when {
            !SystemInfo.isWindows -> false
            else -> try {
                isWsl
                true
            } catch (e: NoClassDefFoundError) {
                false
            }
        }.also { wslSdkIsSupported = it }
    }

private var intellijLspClientSupportedValue: Boolean? = null
val intellijLspClientSupported: Boolean
    get() {
        if (intellijLspClientSupportedValue is Boolean) {
            return intellijLspClientSupportedValue as Boolean
        }
        return try {
            @Suppress("UnstableApiUsage")
            LspServerSupportProvider
            intellijLspClientSupportedValue = true
            true
        } catch (e: NoClassDefFoundError) {
            intellijLspClientSupportedValue = false
            false
        }
    }

private var lsp4ijSupportedValue: Boolean? = null
val lsp4ijSupported: Boolean
    get() {
        if (lsp4ijSupportedValue is Boolean) {
            return lsp4ijSupportedValue as Boolean
        }
        return try {
            com.redhat.devtools.lsp4ij.LanguageServerManager.StartOptions.DEFAULT
            lsp4ijSupportedValue = true
            true
        } catch (e: NoClassDefFoundError) {
            lsp4ijSupportedValue = false
            false
        }
    }

val lspSupported: Boolean get() = intellijLspClientSupported || lsp4ijSupported

fun detectRuffExecutable(
    project: Project,
    ruffConfigService: RuffConfigService,
    lsp: Boolean,
    ruffCacheService: RuffCacheService
): File? {
    project.pythonSdk?.let {
        findRuffExecutableInSDK(it, lsp)
    }.let {
        when {
            lsp -> ruffCacheService.setProjectRuffLspExecutablePath(it?.absolutePath)
            else -> ruffCacheService.setProjectRuffExecutablePath(it?.absolutePath)
        }
        it
    }?.let { return it }

    when (lsp) {
        true -> ruffConfigService.globalRuffLspExecutablePath
        false -> ruffConfigService.globalRuffExecutablePath
    }?.let { File(it) }?.takeIf { it.exists() }?.let { return it }

    return findGlobalRuffExecutable(lsp).let {
        when (lsp) {
            true -> ruffConfigService.globalRuffLspExecutablePath = it?.absolutePath
            false -> ruffConfigService.globalRuffExecutablePath = it?.absolutePath
        }
        it
    }
}

val Sdk.isWsl: Boolean get() = (sdkAdditionalData as? PyTargetAwareAdditionalData)?.targetEnvironmentConfiguration is WslTargetEnvironmentConfiguration

fun findRuffExecutableInSDK(sdk: Sdk, lsp: Boolean): File? {
    return when {
        sdk.wslIsSupported && sdk.isWsl -> {
            val additionalData = sdk.sdkAdditionalData as? PyTargetAwareAdditionalData ?: return null
            val distribution =
                (additionalData.targetEnvironmentConfiguration as? WslTargetEnvironmentConfiguration)?.distribution
                    ?: return null
            val homeParent = sdk.homePath?.let { File(it) }?.parent ?: return null
            File(distribution.getWindowsPath(homeParent), getRuffWlsCommand(lsp))
        }

        (sdk.sdkAdditionalData as? PythonSdkAdditionalData)?.flavor is CondaEnvSdkFlavor ->
            when {
                SystemInfo.isWindows -> sdk.homeDirectory?.parent // {python_dir}/python.exe
                else -> sdk.homeDirectory?.parent?.parent // {python_dir}/bin/python
            }?.path?.let { getRuffExecutableInConda(it, lsp) }

        else -> {
            val parent = sdk.homeDirectory?.parent?.path
            parent?.let { File(it, getRuffCommand(lsp)) }
        }
    }?.takeIf { it.exists() }
}

fun findRuffExecutableInUserSite(lsp: Boolean): File? = File(getUserSiteRuffPath(lsp)).takeIf { it.exists() }

fun getRuffExecutableInConda(condaHomeDir: String, lsp: Boolean): File {
    return File(condaHomeDir + File.separator + SCRIPT_DIR + File.separator, getRuffCommand(lsp))
}

fun findRuffExecutableInConda(condaHomeDir: String, lsp: Boolean): File? {
    return getRuffExecutableInConda(condaHomeDir, lsp).takeIf { it.exists() }
}

fun findRuffExecutableInConda(lsp: Boolean): File? {
    val condaExecutable = PyCondaPackageService.getCondaExecutable(null) ?: return null
    val condaDir = File(condaExecutable).parentFile.parent
    return findRuffExecutableInConda(condaDir, lsp)
}

fun findGlobalRuffExecutable(lsp: Boolean): File? =
    PathEnvironmentVariableUtil.findInPath(if (lsp) RUFF_LSP_COMMAND else RUFF_COMMAND) ?: findRuffExecutableInUserSite(
        lsp
    )
    ?: findRuffExecutableInConda(lsp)

val PsiFile.isApplicableTo: Boolean
    get() = when {
        InjectedLanguageManager.getInstance(project).isInjectedFragment(this) -> false
        else -> language.isKindOf(PythonLanguage.getInstance())
    }
val VirtualFile.isApplicableTo: Boolean
    get() = this.extension in SUPPORTED_FILE_EXTENSIONS

fun getRuffExecutable(
    project: Project,
    ruffConfigService: RuffConfigService,
    lsp: Boolean,
    ruffCacheService: RuffCacheService
): File? {
    with(ruffConfigService) {
        return when {
            lsp -> when {
                alwaysUseGlobalRuff -> globalRuffLspExecutablePath
                else -> ruffCacheService.getProjectRuffLspExecutablePath() ?: globalRuffLspExecutablePath
            }

            else -> when {
                alwaysUseGlobalRuff -> globalRuffExecutablePath
                else -> ruffCacheService.getProjectRuffExecutablePath() ?: globalRuffExecutablePath
            }
        }?.let { File(it) }?.takeIf { it.exists() } ?: detectRuffExecutable(
            project, ruffConfigService, lsp, ruffCacheService
        )
    }
}

fun getConfigArgs(ruffConfigService: RuffConfigService): List<String>? {
    val config = ruffConfigService.ruffConfigPath?.let { File(it) }?.takeIf { it.exists() } ?: return null
    return listOf(CONFIG_ARG, config.absolutePath)
}

fun isInspectionEnabled(project: Project): Boolean {
    val inspectionProfileManager = ProjectInspectionProfileManager.getInstance(project)

    val toolWrapper = InspectionToolRegistrar.getInstance().createTools()
        .find { it.shortName == RuffInspection.INSPECTION_SHORT_NAME } ?: return false
    return inspectionProfileManager.currentProfile.isToolEnabled(toolWrapper.displayKey)
}

fun runCommand(
    commandArgs: CommandArgs, stdin: ByteArray? = null
): String? = runCommand(
    commandArgs.executable,
    commandArgs.project,
    stdin ?: commandArgs.stdin,
    *commandArgs.args.toTypedArray()
)

private fun getGeneralCommandLine(command: List<String>, projectPath: String?): GeneralCommandLine =
    GeneralCommandLine(command).withWorkDirectory(projectPath).withCharset(Charsets.UTF_8)

fun getGeneralCommandLine(executable: File, project: Project?, vararg args: String): GeneralCommandLine? {
    val projectPath = project?.basePath ?: return null
    if (!WslPath.isWslUncPath(executable.path)) {
        return getGeneralCommandLine(listOf(executable.path) + args, projectPath)
    }

    try {
        val windowsUncPath = WslPath.parseWindowsUncPath(executable.path)
        if (windowsUncPath == null) {
            RuffLoggingService.log(
                project,
                "Failed to parse WSL path: ${executable.path}",
                ConsoleViewContentType.ERROR_OUTPUT
            )

            return getGeneralCommandLine(listOf(executable.path) + args, projectPath)
        }

        val configArgIndex = args.indexOf(CONFIG_ARG)
        val injectedArgs = if (configArgIndex != -1 && configArgIndex < args.size - 1) {
            val configPathIndex = configArgIndex + 1
            val configPath = args[configPathIndex]

            val windowsUncConfigPath = try {
                WslPath.parseWindowsUncPath(configPath)?.linuxPath ?: configPath
            } catch (e: Exception) {
                RuffLoggingService.log(
                    project,
                    "Error parsing config path: ${e.message}",
                    ConsoleViewContentType.ERROR_OUTPUT
                )
                configPath
            }

            args.toMutableList().apply { this[configPathIndex] = windowsUncConfigPath }.toTypedArray()
        } else {
            args
        }

        val commandLine = getGeneralCommandLine(listOf(windowsUncPath.linuxPath) + injectedArgs.toList(), projectPath)
        val options = WSLCommandLineOptions()
        options.setExecuteCommandInShell(false)
        options.setLaunchWithWslExe(true)
        return windowsUncPath.distribution.patchCommandLine<GeneralCommandLine>(commandLine, project, options)
    } catch (e: Exception) {
        RuffLoggingService.log(project, "WSL command line error: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)

        try {
            return getGeneralCommandLine(listOf(executable.path) + args, projectPath)
        } catch (e2: Exception) {
            RuffLoggingService.log(
                project, "Failed to create fallback command line: ${e2.message}",
                ConsoleViewContentType.ERROR_OUTPUT
            )
            return null
        }
    }
}

fun runCommand(
    executable: File, project: Project?, stdin: ByteArray?, vararg args: String
): String? {
    RuffLoggingService.log(project!!, "Executing: ${executable.path} ${args.joinToString(" ")}")

    val indicator = ProgressManager.getInstance().progressIndicator
    val handler = getGeneralCommandLine(executable, project, *args)
        ?.let { CapturingProcessHandler(it) } ?: return null

    val result = try {
        with(handler) {
            if (stdin is ByteArray) {
                with(processInput) {
                    try {
                        write(stdin)
                        close()
                    } catch (_: IOException) {
                        throw RunCanceledByUserException()
                    } catch (_: IOError) {
                        throw RunCanceledByUserException()
                    }
                }
            }

            when {
                indicator != null -> {
                    addProcessListener(IndicatedProcessOutputListener(indicator))
                    runProcessWithProgressIndicator(indicator)
                }

                else -> runProcess()
            }
        }
    } finally {
        handler.destroyProcess()
    }
    return with(result) {
        RuffLoggingService.log(project, "Command stdout: $stdout")
        if (stderr.isNotBlank()) {
            RuffLoggingService.log(project, "Command stderr: $stderr", ConsoleViewContentType.ERROR_OUTPUT)
        }
        when {
            isCancelled -> throw RunCanceledByUserException()

            exitCode != 0 -> {
                when {
                    stderr.startsWith("error: TOML parse error at line ", ignoreCase = false) -> {
                        throw TOMLParseException()
                    }

                    stderr.startsWith("error: unexpected argument '--output-format' found", ignoreCase = false) -> {
                        throw UnexpectedNewArgumentException(NewArgument.OUTPUT_FORMAT)
                    }

                    Regex("(-|format):\\d+:\\d+: E902 No such file or directory \\(os error 2\\)\n").findAll(stdout)
                        .count() == 2 -> {
                        throw UnexpectedNewArgumentException(NewArgument.FORMAT)
                    }

                    else -> throw PyExecutionException(
                        "Error Running Ruff", executable.path, args.asList(), stdout, stderr, exitCode, emptyList()
                    )
                }
            }

            else -> stdout
        }
    }
}

data class SourceFile(
    private val psiFile: PsiFile,
    private val textRange: TextRange? = null,
    private val reloadText: Boolean = false
) {
    val text: String? by lazy {
        val text = when (reloadText) {
            true -> currentText
            else -> psiFile.text
        } ?: return@lazy null
        if (textRange == null) return@lazy text
        if (textRange.endOffset <= text.length) textRange.substring(text)
        text.substring(textRange.startOffset, text.length)
    }
    val currentText: String? get() = documentationManager.getDocument(psiFile)?.text
    private val documentationManager: PsiDocumentManager get() = PsiDocumentManager.getInstance(project)
    val project: Project get() = psiFile.project
    val virtualFile: VirtualFile? get() = psiFile.virtualFile

    val name: String get() = psiFile.name
    val asStdin: ByteArray? get() = text?.let {convertToBytes(it) }
    val asCurrentTextStdin: ByteArray? get() = currentText?.let {convertToBytes(it) }
    private fun convertToBytes(text: String): ByteArray = text.toCharArray().toByteArrayAndClear()
    fun hasSameContentAsDocument(document: Document): Boolean = document.charsSequence.contentEquals(text)
}

val PsiFile.sourceFile: SourceFile get() = SourceFile(this)

fun PsiFile.getSourceFile(textRange: TextRange? = null, reloadText: Boolean = false): SourceFile =
    SourceFile(this, textRange, reloadText)

fun runRuff(sourceFile: SourceFile, args: List<String>): String? =
    generateCommandArgs(sourceFile, args, true)?.let { runRuff(it) }

fun runRuff(project: Project, args: List<String>, withoutConfig: Boolean = false): String? =
    generateCommandArgs(project, null, args, false, withoutConfig)?.let { runRuff(it) }


data class CommandArgs(
    val executable: File, val project: Project,
    val stdin: ByteArray?, val args: List<String>,
)

fun generateCommandArgs(sourceFile: SourceFile, args: List<String>, setStdin: Boolean): CommandArgs? =
    generateCommandArgs(
        sourceFile.project,
        if (setStdin) sourceFile.asStdin else null,
        args + getStdinFileNameArgs(sourceFile),
        true
    )

fun generateCommandArgs(
    project: Project,
    stdin: ByteArray?,
    args: List<String>,
    addStdinOption: Boolean,
    withoutConfig: Boolean = false
): CommandArgs? {
    val ruffConfigService = project.configService
    val ruffCacheService = RuffCacheService.getInstance(project)
    val executable =
        getRuffExecutable(project, ruffConfigService, false, ruffCacheService) ?: return null
    val customConfigArgs = if (withoutConfig) null else ruffConfigService.ruffConfigPath?.let {
        args + listOf(CONFIG_ARG, it)
    }
    return CommandArgs(
        executable,
        project,
        stdin,
        (customConfigArgs ?: args) + if (stdin == null && !addStdinOption) listOf() else listOf("-")
    )
}


class TOMLParseException : ExecutionException("TOML parse error")

enum class NewArgument(val argument: String) {
    OUTPUT_FORMAT("--output-format"),
    FORMAT("--format")
}

open class UnexpectedArgumentException(argument: String) : ExecutionException("Unexpected argument: $argument")
class UnexpectedNewArgumentException(newArgument: NewArgument) : UnexpectedArgumentException(newArgument.argument)

fun runRuff(commandArgs: CommandArgs, stdin: ByteArray? = null): String? {
    return try {
        runCommand(commandArgs, stdin)
    } catch (_: RunCanceledByUserException) {
        null
    } catch (_: TOMLParseException) {
        null
    } catch (e: UnexpectedNewArgumentException) {
        RuffCacheService.getInstance(commandArgs.project).clearVersion()
        null
    }
}


inline fun <reified T> runRuffInBackground(
    sourceFile: SourceFile, args: List<String>, crossinline callback: (String?) -> T
): ProgressIndicator? =
    runRuffInBackground(
        sourceFile.project,
        sourceFile.asStdin,
        args + getStdinFileNameArgs(sourceFile),
        "running ruff ${sourceFile.name}",
        callback
    )

inline fun <reified T> runRuffInBackground(
    project: Project, stdin: ByteArray?, args: List<String>, description: String, crossinline callback: (String?) -> T
): ProgressIndicator? {
    val commandArgs = generateCommandArgs(project, stdin, args, true) ?: return null
    val task = object : Task.Backgroundable(project, StringUtil.toTitleCase(description), true) {
        override fun run(indicator: ProgressIndicator) {
            indicator.text = "$description..."
            val result: String? = try {
                runRuff(commandArgs)
            } catch (e: ExecutionException) {
                showSdkExecutionException(project.pythonSdk, e, "Error Running Ruff")
                null
            }
            callback(result)
        }
    }
    ProgressManager.getInstance().let {
        it.run(task)
        return it.progressIndicator
    }
}

inline fun <reified T> executeOnPooledThread(
    project: Project,
    defaultResult: T, timeoutSeconds: Long = 30, crossinline action: () -> T
): T {
    val future = try {
        ApplicationManager.getApplication().executeOnPooledThread<T> {
            try {
                action.invoke()
            } catch (e: PyExecutionException) {
                defaultResult
            } catch (e: ProcessNotCreatedException) {
                defaultResult
            }
        }
    } catch (e: Exception) {
        return defaultResult
    }

    return try {
        future.get(timeoutSeconds, TimeUnit.SECONDS)
    } catch (e: TimeoutException) {
        RuffLoggingService.log(
            project,
            "Task timed out after $timeoutSeconds seconds",
            ConsoleViewContentType.ERROR_OUTPUT
        )
        future.cancel(true)
        defaultResult
    } catch (e: Exception) {
        future.cancel(true)
        defaultResult
    }
}

inline fun <reified T> runReadActionOnPooledThread(
    project: Project,
    defaultResult: T, timeoutSeconds: Long = 30, crossinline action: () -> T
): T = ApplicationManager.getApplication().runReadAction<T> {
    executeOnPooledThread(project, defaultResult, timeoutSeconds, action)
}


inline fun executeOnPooledThread(crossinline action: () -> Unit) =
    ApplicationManager.getApplication().executeOnPooledThread {
        try {
            action.invoke()
        } catch (_: PyExecutionException) {
        } catch (_: ProcessNotCreatedException) {
        }
    }


fun parseJsonResponse(response: String): List<Result> = try {
    json.decodeFromString(response)
} catch (_: SerializationException) {
    listOf()
} catch (_: Exception) {
    listOf()
}

fun VirtualFile.isInProjectDir(project: Project): Boolean =
    project.basePath?.let { canonicalPath?.startsWith(it) } ?: false

fun getProjectRelativeFilePath(project: Project, virtualFile: VirtualFile): String? {
    val projectBasePath = project.basePath?.let { Paths.get(it) } ?: return null
    val filePath = virtualFile.canonicalPath?.let { Paths.get(it) } ?: return null
    if (!project.modules.any { module -> module.basePath?.let { filePath.startsWith(it) } == true }) return null
    return projectBasePath.relativize(filePath).pathString
}

fun getStdinFileNameArgs(sourceFile: SourceFile): List<String> {
    val virtualFile = sourceFile.virtualFile ?: return emptyList()
    val pythonSdk = sourceFile.project.pythonSdk

    if (pythonSdk?.isWsl == true) {
        val wslTargetConfig =
            (pythonSdk.sdkAdditionalData as? PyTargetAwareAdditionalData)
                ?.targetEnvironmentConfiguration as? WslTargetEnvironmentConfiguration
        val wslDistribution = wslTargetConfig?.distribution
        val wslPath = try {
            wslDistribution?.getWslPath(virtualFile.toNioPath())
        } catch (_: Exception) {
            null
        } ?: virtualFile.canonicalPath ?: return emptyList()
        return listOf("--stdin-filename", wslPath)
    }

    return getProjectRelativeFilePath(sourceFile.project, virtualFile)?.let { projectRelativeFilePath ->
        listOf("--stdin-filename", projectRelativeFilePath)
    } ?: emptyList()
}

fun Document.getStartEndRange(startLocation: Location, endLocation: Location, offset: Int): TextRange {
    val start = getLineStartOffset(startLocation.row - 1) + startLocation.column + offset
    val lastLine = endLocation.row - 1
    val end: Int = when (lineCount) {
        lastLine -> textLength
        else -> getLineStartOffset(lastLine) + endLocation.column + offset
    }
    return TextRange(start, end)
}

fun checkFixResult(sourceFile: SourceFile, fixResult: String?): String? {
    if (fixResult == null) return null
    if (fixResult.isNotBlank()) return fixResult
    val noFixArgs = sourceFile.project.NO_FIX_ARGS ?: return null
    val noFixResult = runRuff(sourceFile, noFixArgs) ?: return null

    // check the file is excluded
    if (noFixResult.trim() == "[]") return null
    return fixResult
}

//val PyExecutionException.failureReasonExitCode: Int get() {
//    return when (val failureReason = failureReason) {
//        is FailureReason.ExecutionFailed -> failureReason.output.exitCode
//        else -> -1
//    }
//}
fun checkFormatResult(sourceFile: SourceFile, formatResult: String?): String? {
    if (formatResult == null) return null
    if (formatResult.isNotBlank()) return formatResult

    try {
        runRuff(sourceFile, FORMAT_CHECK_ARGS)
    } catch (e: PyExecutionException) {
        if (e.exitCode == 1) {
            return formatResult
        }
    //        if (e.failureReasonExitCode == 1) {
//            return formatResult
//        }
    }
    return null
}

fun fix(sourceFile: SourceFile): String? {
    val fixResult = runRuff(sourceFile, sourceFile.project.FIX_ARGS) ?: return null
    return checkFixResult(sourceFile, fixResult)
}

fun format(sourceFile: SourceFile): String? {
    val formatResult = runRuff(sourceFile, FORMAT_ARGS) ?: return null
    return checkFormatResult(sourceFile, formatResult)
}

val NOQA_COMMENT_PATTERN: Pattern = Pattern.compile(
    "# noqa(?::[\\s]?(?<codes>([A-Z]+[0-9]+(?:[,\\s]+)?)+))?.*",
    Pattern.CASE_INSENSITIVE
)

data class NoqaCodes(val codes: List<String>?, val noqaStartOffset: Int, val noqaEndOffset: Int)

fun extractNoqaCodes(comment: PsiComment): NoqaCodes? {
    val commentText = comment.text ?: return null
    val noqaOffset = commentText.lowercase().indexOf("# noqa")
    if (noqaOffset == -1) return null
    val noqaStartOffset = comment.textRange.startOffset + noqaOffset
    val matcher = NOQA_COMMENT_PATTERN.matcher(commentText.substring(noqaOffset))
    if (!matcher.find()) {
        return NoqaCodes(null, noqaStartOffset, noqaStartOffset + "# noqa".length)
    }
    val codes =
        matcher.group("codes")?.split("[,\\s]+".toRegex())?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()
    return NoqaCodes(codes, noqaStartOffset, noqaStartOffset + matcher.end())
}

const val PY_PROJECT_TOML: String = "pyproject.toml"
const val RUFF_TOML: String = "ruff.toml"
const val RUFF_TOML_SUFFIX: String = ".ruff.toml"
private val RUFF_CONFIG: List<String> = listOf(PY_PROJECT_TOML, RUFF_TOML)
val VirtualFile.isRuffConfig: Boolean
    get() = name in RUFF_CONFIG || name.endsWith(RUFF_TOML_SUFFIX)
val Project.configService: RuffConfigService get() = RuffConfigService.getInstance(this)


/**
 * Returns the 1-indexed line and column numbers for the given offset in the string.
 *
 * @param offset the character offset within the string.
 * @return a Pair where the first component is the line number and the second is the column number.
 */
fun String.getLineAndColumn(offset: Int): Pair<Int, Int> {
    // Count the number of newline characters before the offset to determine the line number.
    val line = this.substring(0, offset).count { it == '\n' } + 1
    // Determine the column number by finding the position of the last newline.
    val lastNewline = this.lastIndexOf('\n', offset - 1)
    val column = if (lastNewline == -1) offset + 1 else offset - lastNewline
    return line to column
}

/**
 * Converts the given TextRange into a formatted string "<start_line>:<start_column>-<end_line>:<end_column>"
 * using the provided full text to calculate the line and column numbers.
 *
 * @param text the full text content from which the TextRange was obtained.
 * @return a formatted string representing the range.
 */
fun TextRange.formatRange(text: String): String {
    val (startLine, startColumn) = text.getLineAndColumn(startOffset)
    val (endLine, endColumn) = text.getLineAndColumn(endOffset)
    return "$startLine:$startColumn-$endLine:$endColumn"
}


fun TextRange.formatRangeArgs(text: String): List<String> = FORMAT_RANGE_ARGS + listOf(formatRange(text))
