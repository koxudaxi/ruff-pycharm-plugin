package com.koxudaxi.ruff

import com.intellij.credentialStore.toByteArrayAndClear
import com.intellij.execution.ExecutionException
import com.intellij.execution.RunCanceledByUserException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.wsl.WSLCommandLineOptions
import com.intellij.execution.wsl.WslPath
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
import com.intellij.psi.PsiFile
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.packaging.IndicatedProcessOutputListener
import com.jetbrains.python.packaging.PyCondaPackageService
import com.jetbrains.python.packaging.PyExecutionException
import com.jetbrains.python.sdk.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.SystemDependent
import java.io.File
import java.io.IOError
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

val RUFF_COMMAND = when {
    SystemInfo.isWindows -> "ruff.exe"
    else -> "ruff"
}

val SCRIPT_DIR = when {
    SystemInfo.isWindows -> "Scripts"
    else -> "bin"
}

val USER_SITE_RUFF_PATH = PythonSdkUtil.getUserSite() + File.separator + "bin" + File.separator + RUFF_COMMAND

val json = Json { ignoreUnknownKeys = true }

val ARGS_BASE = listOf("--exit-zero", "--no-cache", "--force-exclude")
val FIX_ARGS = ARGS_BASE + listOf("--fix")
val NO_FIX_ARGS = ARGS_BASE + listOf("--no-fix", "--format", "json")

fun detectRuffExecutable(project: Project, ruffConfigService: RuffConfigService): File? {
    project.pythonSdk?.let {
        findRuffExecutableInSDK(it)
    }.let {
        ruffConfigService.projectRuffExecutablePath = it?.absolutePath
        it
    }?.let { return it }

    ruffConfigService.globalRuffExecutablePath?.let { File(it) }?.takeIf { it.exists() }?.let { return it }

    return findGlobalRuffExecutable().let {
        ruffConfigService.globalRuffExecutablePath = it?.absolutePath
        it
    }
}

fun findRuffExecutableInSDK(sdk: Sdk): File? =
    sdk.homeDirectory?.parent?.let { File(it.path, RUFF_COMMAND) }?.takeIf { it.exists() }

fun findRuffExecutableInUserSite(): File? = File(USER_SITE_RUFF_PATH).takeIf { it.exists() }

fun findRuffExecutableInConda(): File? {
    val condaExecutable = PyCondaPackageService.getCondaExecutable(null) ?: return null
    val condaDir = File(condaExecutable).parentFile.parent
    return File(condaDir + File.separator + SCRIPT_DIR + File.separator, RUFF_COMMAND).takeIf { it.exists() }
}

fun findGlobalRuffExecutable(): File? =
    PathEnvironmentVariableUtil.findInPath(RUFF_COMMAND) ?: findRuffExecutableInUserSite()
    ?: findRuffExecutableInConda()

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

            exitCode != 0 -> throw PyExecutionException(
                "Error Running Ruff", executable.path, args.asList(), stdout, stderr, exitCode, emptyList()
            )

            else -> stdout
        }
    }
}

fun runRuff(psiFile: PsiFile, args: List<String>): String? =
    runRuff(generateCommandArgs(psiFile, args))

data class CommandArgs(
    val executable: File, val projectPath: @SystemDependent String?,
    val stdin: ByteArray?, val args: List<String>,
)

fun generateCommandArgs(psiFile: PsiFile, args: List<String>): CommandArgs =
    generateCommandArgs(
        psiFile.project,
        psiFile.textToCharArray().toByteArrayAndClear(),
        args + getStdinFileNameArgs(psiFile)
    )

fun generateCommandArgs(
    project: Project,
    stdin: ByteArray?,
    args: List<String>
): CommandArgs {
    val ruffConfigService = RuffConfigService.getInstance(project)
    val executable =
        ruffConfigService.ruffExecutablePath?.let { File(it) }?.takeIf { it.exists() } ?: detectRuffExecutable(
            project, ruffConfigService
        ) ?: throw PyExecutionException("Cannot find Ruff", "ruff", emptyList(), ProcessOutput())
    val customConfigArgs = ruffConfigService.ruffConfigPath?.let {
        listOf("--config", it) + args
    }
    return CommandArgs(executable, project.basePath, stdin, customConfigArgs ?: args)
}

fun runRuff(commandArgs: CommandArgs): String? {
    return try {
        runCommand(commandArgs)
    } catch (_: RunCanceledByUserException) {
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
    val commandArgs = generateCommandArgs(project, stdin, args)
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
            "--stdin-filename", projectRelativeFilePath, "-"
        )
    }
} ?: listOf("-")

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
    val noFixResult = runRuff(pyFile, NO_FIX_ARGS) ?: return null

    // check the file is excluded
    if (noFixResult == "[]\n") return null
    return fixResult
}

fun format(pyFile: PsiFile): String? {
    val fixResult = runRuff(pyFile, FIX_ARGS) ?: return null
    return checkFixResult(pyFile, fixResult)
}