package com.koxudaxi.ruff

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunCanceledByUserException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.execution.process.ProcessOutput
import com.intellij.ide.util.PropertiesComponent
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.packaging.IndicatedProcessOutputListener
import com.jetbrains.python.packaging.PyExecutionException
import com.jetbrains.python.sdk.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.SystemDependent
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

const val RUFF_PATH_SETTING: String = "PyCharm.Ruff.Path"
const val RUFF_COMMAND: String = "ruff"

val json = Json { ignoreUnknownKeys = true }

var PropertiesComponent.ruffPath: @SystemDependent String?
    get() = getValue(RUFF_PATH_SETTING)
    set(value) {
        setValue(RUFF_PATH_SETTING, value)
    }

fun getRuffExecutable(): File? =
    PropertiesComponent.getInstance().ruffPath?.let { File(it) }?.takeIf { it.exists() } ?: detectRuffExecutable()

fun getRuffExecutableInSDK(sdk: Sdk): File? =
    sdk.homeDirectory?.parent?.let { File(it.path, RUFF_COMMAND) }?.takeIf { it.exists() }

fun detectRuffExecutable(): File? {
    return PathEnvironmentVariableUtil.findInPath(RUFF_COMMAND)
}

val PsiFile.isApplicableTo: Boolean
    get() =
        when {
            InjectedLanguageManager.getInstance(project).isInjectedFragment(this) -> false
            else -> language.isKindOf(PythonLanguage.getInstance())
        }


fun runCommand(
    executable: File,
    projectPath: @SystemDependent String?,
    stdin: ByteArray?,
    vararg args: String
): String {
    val command = listOf(executable.path) + args
    val commandLine = GeneralCommandLine(command).withWorkDirectory(projectPath)
    val handler = CapturingProcessHandler(commandLine)
    val indicator = ProgressManager.getInstance().progressIndicator
    val result = with(handler) {
        if (stdin is ByteArray) {
            with(processInput) {
                write(stdin)
                close()
            }
        }

        when {
            indicator != null -> {
                addProcessListener(IndicatedProcessOutputListener(indicator))
                runProcessWithProgressIndicator(indicator)
            }

            else ->
                runProcess()
        }
    }
    return with(result) {
        when {
            isCancelled ->
                throw RunCanceledByUserException()

            exitCode != 0 ->
                throw PyExecutionException(
                    "Error Running Ruff", executable.path, args.asList(),
                    stdout, stderr, exitCode, emptyList()
                )

            else -> stdout
        }
    }
}

fun runRuff(module: Module, stdin: ByteArray?, vararg args: String): String {
    val executable = module.pythonSdk?.let { getRuffExecutableInSDK(it) } ?: getRuffExecutable()
    ?: throw PyExecutionException("Cannot find Ruff", "ruff", emptyList(), ProcessOutput())

    return runCommand(executable, module.basePath, stdin, *args)
}

inline fun <reified T> runRuffInBackground(
    module: Module,
    stdin: ByteArray?,
    args: List<String>,
    description: String,
    crossinline callback: (String?) -> T
): ProgressIndicator? {
    val task = object : Task.Backgroundable(module.project, StringUtil.toTitleCase(description), true) {
        override fun run(indicator: ProgressIndicator) {
            indicator.text = "$description..."
            val result: String? = try {
                runRuff(module, stdin, *args.toTypedArray())
            } catch (_: RunCanceledByUserException) {
                null
            } catch (e: ExecutionException) {
                showSdkExecutionException(module.pythonSdk, e, "Error Running Ruff")
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
    defaultResult: T,
    timeoutSeconds: Long = 30,
    crossinline action: () -> T
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

fun parseJsonResponse(response: String): List<Result> = json.decodeFromString(response)
