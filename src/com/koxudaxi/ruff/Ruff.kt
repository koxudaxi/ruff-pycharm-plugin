package com.koxudaxi.ruff

import com.intellij.credentialStore.toByteArrayAndClear
import com.intellij.execution.ExecutionException
import com.intellij.execution.RunCanceledByUserException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessNotCreatedException
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
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.packaging.IndicatedProcessOutputListener
import com.jetbrains.python.packaging.PyCondaPackageService
import com.jetbrains.python.packaging.PyExecutionException
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkUtil
import com.jetbrains.python.sdk.flavors.conda.CondaEnvSdkFlavor
import com.jetbrains.python.sdk.pythonSdk
import com.jetbrains.python.sdk.showSdkExecutionException
import com.jetbrains.python.target.PyTargetAwareAdditionalData
import com.jetbrains.python.wsl.isWsl
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.SystemDependent
import java.io.File
import java.io.IOError
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.regex.Pattern

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

val ruffVersionCache: HashMap<String, RuffVersion> = hashMapOf()

fun getRuffCommand(lsp: Boolean) = if (lsp) RUFF_LSP_COMMAND else RUFF_COMMAND

fun getRuffWlsCommand(lsp: Boolean) = if (lsp) WSL_RUFF_LSP_COMMAND else WSL_RUFF_COMMAND


val SCRIPT_DIR = when {
    SystemInfo.isWindows -> "Scripts"
    else -> "bin"
}

fun getUserSiteRuffPath(lsp: Boolean) = PythonSdkUtil.getUserSite() + File.separator + "bin" + File.separator + getRuffCommand(lsp)


val json = Json { ignoreUnknownKeys = true }

val ARGS_BASE = listOf("--exit-zero", "--no-cache", "--force-exclude")
val FIX_ARGS = ARGS_BASE + listOf("--fix")
val NO_FIX_FORMAT_ARGS = ARGS_BASE + listOf("--no-fix", "--format", "json")
val NO_FIX_OUTPUT_FORMAT_ARGS = ARGS_BASE + listOf("--no-fix", "--output-format", "json")
val FORMAT_ARGS = listOf("format", "--force-exclude", "--quiet")
val FORMAT_CHECK_ARGS = FORMAT_ARGS + listOf("--check")
val Project.NO_FIX_ARGS get() = if (RuffCacheService.hasOutputFormat(this)) NO_FIX_OUTPUT_FORMAT_ARGS else NO_FIX_FORMAT_ARGS

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

private var lspIsSupportedValue: Boolean? = null
val lspIsSupported: Boolean
    get() {
        if (lspIsSupportedValue is Boolean) {
            return lspIsSupportedValue as Boolean
        }
        return try {
                LspServerSupportProvider
                true
            } catch (e: NoClassDefFoundError) {
                false
            }.also { lspIsSupportedValue = it }
    }


fun detectRuffExecutable(project: Project, ruffConfigService: RuffConfigService, lsp: Boolean): File? {
    project.pythonSdk?.let {
        findRuffExecutableInSDK(it, lsp)
    }.let {
        when {
            lsp -> ruffConfigService.projectRuffExecutablePath = it?.absolutePath
            else -> ruffConfigService.projectRuffLspExecutablePath = it?.absolutePath
        }
        it
    }?.let { return it }

    when(lsp) {
        true -> ruffConfigService.globalRuffExecutablePath
        false -> ruffConfigService.globalRuffLspExecutablePath
    }?.let { File(it) }?.takeIf { it.exists() }?.let { return it }

    return findGlobalRuffExecutable(lsp).let {
        when(lsp) {
            true -> ruffConfigService.globalRuffExecutablePath = it?.absolutePath
            false -> ruffConfigService.globalRuffLspExecutablePath = it?.absolutePath
        }
        it
    }
}
fun findRuffExecutableInSDK(sdk: Sdk, lsp: Boolean): File? {
    return when {
        sdk.wslIsSupported && sdk.isWsl -> {
            val additionalData = sdk.sdkAdditionalData as? PyTargetAwareAdditionalData ?: return null
            val distribution = (additionalData.targetEnvironmentConfiguration as? WslTargetEnvironmentConfiguration)?.distribution ?: return null
            val homeParent = sdk.homePath?.let { File(it) }?.parent ?: return null
            File(distribution.getWindowsPath(homeParent), getRuffWlsCommand(lsp))
        }
       (sdk.sdkAdditionalData as? PythonSdkAdditionalData)?.flavor is CondaEnvSdkFlavor ->
           sdk.homeDirectory?.parent?.path?.let { findRuffExecutableInConda(it, lsp) }
        else -> {
            val parent = sdk.homeDirectory?.parent?.path
            parent?.let {  File(it, getRuffCommand(lsp)) }}
    }?.takeIf { it.exists() }
}
fun findRuffExecutableInUserSite(lsp: Boolean): File? = File(getUserSiteRuffPath(lsp)).takeIf { it.exists() }

fun findRuffExecutableInConda(condaHomeDir: String, lsp: Boolean): File? {
    return File(condaHomeDir + File.separator + SCRIPT_DIR + File.separator, getRuffCommand(lsp)).takeIf { it.exists() }
}
fun findRuffExecutableInConda(lsp: Boolean): File? {
    val condaExecutable = PyCondaPackageService.getCondaExecutable(null) ?: return null
    val condaDir = File(condaExecutable).parentFile.parent
    return findRuffExecutableInConda(condaDir, lsp)
}

fun findGlobalRuffExecutable(lsp: Boolean): File? =
    PathEnvironmentVariableUtil.findInPath(if (lsp) RUFF_LSP_COMMAND else RUFF_COMMAND) ?: findRuffExecutableInUserSite(lsp)
    ?: findRuffExecutableInConda(lsp)

val PsiFile.isApplicableTo: Boolean
    get() = when {
        InjectedLanguageManager.getInstance(project).isInjectedFragment(this) -> false
        else -> language.isKindOf(PythonLanguage.getInstance())
    }

fun runCommand(
    commandArgs: CommandArgs
): String? = runCommand(
    commandArgs.executable, commandArgs.projectPath, commandArgs.stdin, *commandArgs.args.toTypedArray()
)


fun getGeneralCommandLine(command: List<String>, projectPath: String?): GeneralCommandLine =
    GeneralCommandLine(command).withWorkDirectory(projectPath).withCharset(Charsets.UTF_8)

fun runCommand(
    executable: File, projectPath: @SystemDependent String?, stdin: ByteArray?, vararg args: String
): String? {

    val indicator = ProgressManager.getInstance().progressIndicator
    val handler = if (WslPath.isWslUncPath(executable.path)) {
        val windowsUncPath = WslPath.parseWindowsUncPath(executable.path) ?: return null
        val options = WSLCommandLineOptions()
        options.setExecuteCommandInShell(false)
        val commandLine = getGeneralCommandLine(listOf(windowsUncPath.linuxPath) + args, projectPath)
        windowsUncPath.distribution.patchCommandLine<GeneralCommandLine>(commandLine, null, WSLCommandLineOptions())
    } else {
        getGeneralCommandLine(listOf(executable.path) + args, projectPath)
    }.let { CapturingProcessHandler(it) }

    val result = with(handler) {
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
    return with(result) {
        when {
            isCancelled -> throw RunCanceledByUserException()

            exitCode != 0 -> {
                if (stderr.startsWith("error: TOML parse error at line ", ignoreCase = false)) {
                    throw TOMLParseException()
                }
                throw PyExecutionException(
                    "Error Running Ruff", executable.path, args.asList(), stdout, stderr, exitCode, emptyList()
                )
            }

            else -> stdout
        }
    }
}

fun runRuff(psiFile: PsiFile, args: List<String>): String? =
        generateCommandArgs(psiFile, args)?.let { runRuff(it) }

fun runRuff(project: Project, args: List<String>, withoutConfig: Boolean = false): String? =
    generateCommandArgs(project, null, args, withoutConfig)?.let { runRuff(it) }



data class CommandArgs(
    val executable: File, val projectPath: @SystemDependent String?,
    val stdin: ByteArray?, val args: List<String>,
)

fun generateCommandArgs(psiFile: PsiFile, args: List<String>): CommandArgs? =
    generateCommandArgs(
        psiFile.project,
        psiFile.textToCharArray().toByteArrayAndClear(),
        args + getStdinFileNameArgs(psiFile)
    )

fun generateCommandArgs(
    project: Project,
    stdin: ByteArray?,
    args: List<String>,
    withoutConfig: Boolean = false
): CommandArgs? {
    val ruffConfigService = RuffConfigService.getInstance(project)
    val executable =
        ruffConfigService.ruffExecutablePath?.let { File(it) }?.takeIf { it.exists() } ?: detectRuffExecutable(
            project, ruffConfigService, false
        ) ?: return null
    val customConfigArgs = if (withoutConfig) null else ruffConfigService.ruffConfigPath?.let {
        args + listOf("--config", it)
    }
    return CommandArgs(
        executable,
        project.basePath,
        stdin,
        (customConfigArgs ?: args) + if (stdin == null) listOf() else listOf("-")
    )
}


class TOMLParseException : ExecutionException("TOML parse error")

fun runRuff(commandArgs: CommandArgs): String? {
    return try {
        runCommand(commandArgs)
    } catch (_: RunCanceledByUserException) {
        null
    } catch (_: TOMLParseException) {
        null
    }
}



inline fun <reified T> runRuffInBackground(
    psiFile: PsiFile, args: List<String>, crossinline callback: (String?) -> T
): ProgressIndicator? =
    runRuffInBackground(
        psiFile.project,
        psiFile.textToCharArray().toByteArrayAndClear(),
        args + getStdinFileNameArgs(psiFile),
        "running ruff ${psiFile.name}",
        callback
    )

inline fun <reified T> runRuffInBackground(
    project: Project, stdin: ByteArray?, args: List<String>, description: String, crossinline callback: (String?) -> T
): ProgressIndicator? {
    val commandArgs = generateCommandArgs(project, stdin, args) ?: return null
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
    defaultResult: T, timeoutSeconds: Long = 30, crossinline action: () -> T
): T {
    return try {
        ApplicationManager.getApplication().executeOnPooledThread<T> {
            try {
                action.invoke()
            } catch (e: PyExecutionException) {
                defaultResult
            } catch (e: ProcessNotCreatedException) {
                defaultResult
            }
        }.get(timeoutSeconds, TimeUnit.SECONDS)
    } catch (e: TimeoutException) {
        defaultResult
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
    val canonicalPath = virtualFile.canonicalPath ?: return null
    return project.basePath?.takeIf { canonicalPath.startsWith(it) }?.let {
        canonicalPath.substring(it.length + 1)
    }
}

fun getStdinFileNameArgs(psiFile: PsiFile) = psiFile.virtualFile?.let {
    getProjectRelativeFilePath(psiFile.project, it)?.let { projectRelativeFilePath ->
        listOf(
            "--stdin-filename", projectRelativeFilePath
        )
    }
} ?: listOf()

fun Document.getStartEndRange(startLocation: Location, endLocation: Location, offset: Int): TextRange {
    val start = getLineStartOffset(startLocation.row - 1) + startLocation.column + offset
    val lastLine = endLocation.row - 1
    val end: Int = when (lineCount) {
        lastLine -> textLength
        else -> getLineStartOffset(lastLine) + endLocation.column + offset
    }
    return TextRange(start, end)
}

fun checkFixResult(pyFile: PsiFile, fixResult: String?): String? {
    if (fixResult == null) return null
    if (fixResult.isNotBlank()) return fixResult
    val noFixResult = runRuff(pyFile, pyFile.project.NO_FIX_ARGS) ?: return null

    // check the file is excluded
    if (noFixResult == "[]\n") return null
    return fixResult
}

fun checkFormatResult(pyFile: PsiFile, formatResult: String?): String? {
    if (formatResult == null) return null
    if (formatResult.isNotBlank()) return formatResult

    try {
        runRuff(pyFile, FORMAT_CHECK_ARGS)
    } catch (e: PyExecutionException) {
        if (e.exitCode == 1) {
            return formatResult
        }
    }
    return null
}

fun fix(pyFile: PsiFile): String? {
    val fixResult = runRuff(pyFile, FIX_ARGS) ?: return null
    return checkFixResult(pyFile, fixResult)
}

fun format(pyFile: PsiFile): String? {
    val formatResult = runRuff(pyFile, FORMAT_ARGS) ?: return null
    return checkFormatResult(pyFile, formatResult)
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
    val codes = matcher.group("codes")?.split("[,\\s]+".toRegex())?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()
    return NoqaCodes(codes, noqaStartOffset,noqaStartOffset + matcher.end())
}
