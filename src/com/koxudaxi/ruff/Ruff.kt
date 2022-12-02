package com.koxudaxi.ruff

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunCanceledByUserException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.python.packaging.IndicatedProcessOutputListener
import com.jetbrains.python.packaging.PyExecutionException
import com.jetbrains.python.sdk.*
import org.jetbrains.annotations.SystemDependent
import java.io.File

const val RUFF_PATH_SETTING: String = "PyCharm.Ruff.Path"
const val RUFF_COMMAND: String = "ruff"
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

fun runCommand(executable: File, projectPath: @SystemDependent String?, vararg args: String): String {
    val command = listOf(executable.path) + args
    val commandLine = GeneralCommandLine(command).withWorkDirectory(projectPath)
    val handler = CapturingProcessHandler(commandLine)
    val indicator = ProgressManager.getInstance().progressIndicator
    val result = with(handler) {
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

            else -> stdout.trim()
        }
    }
}

fun runRuff(sdk: Sdk, vararg args: String): String {
    val projectPath = sdk.associatedModulePath
        ?: throw PyExecutionException(
            "Cannot find the project associated with this Ruff environment",
            "Ruff", emptyList(), ProcessOutput()
        )
    val executable = getRuffExecutableInSDK(sdk) ?: getRuffExecutable()
    ?: throw PyExecutionException("Cannot find Ruff", "ruff", emptyList(), ProcessOutput())

    return runCommand(executable, projectPath, *args)
}

fun runRuffInBackground(module: Module, args: List<String>, description: String) {
    val task = object : Task.Backgroundable(module.project, StringUtil.toTitleCase(description), true) {
        override fun run(indicator: ProgressIndicator) {
            val sdk = module.pythonSdk ?: return
            indicator.text = "$description..."
            try {
                runRuff(sdk, *args.toTypedArray())
            } catch (_: RunCanceledByUserException) {
            } catch (e: ExecutionException) {
                showSdkExecutionException(sdk, e, "Error Running Ruff")
            } finally {
                VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
            }
        }
    }
    ProgressManager.getInstance().run(task)
}
