import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCompletionSupport
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.jetbrains.python.sdk.pythonSdk
import com.koxudaxi.ruff.RuffConfigService
import com.koxudaxi.ruff.RuffInspection
import com.koxudaxi.ruff.findRuffExecutableInSDK


class RuffLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        if (!RuffConfigService.getInstance(project).useRuffLsp) return
        if (!isInspectionEnabled(project)) return
        if (file.extension != "py") return

        serverStarter.ensureServerStarted(RuffLspServerDescriptor(project))
    }

    fun isInspectionEnabled(project: Project): Boolean {
        val inspectionProfileManager = ProjectInspectionProfileManager.getInstance(project)

        val toolWrapper = InspectionToolRegistrar.getInstance().createTools()
            .find { it.shortName == RuffInspection.INSPECTION_SHORT_NAME }

        return toolWrapper?.let {
            inspectionProfileManager.currentProfile.isToolEnabled(it.displayKey)
        } ?: false
    }
}


private class RuffLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Ruff") {
    override fun isSupportedFile(file: VirtualFile) = file.extension == "py"

    override fun createCommandLine(): GeneralCommandLine {
        project.pythonSdk?.let {
            findRuffExecutableInSDK(it, true)?.let {
                return GeneralCommandLine(it.absolutePath)
            }
        }
        return GeneralCommandLine("ruff-lsp")
    }

    override val lspGoToDefinitionSupport: Boolean = false
    override val lspCompletionSupport: LspCompletionSupport? = null
}