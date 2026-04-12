import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.koxudaxi.ruff.RuffConfigService
import com.koxudaxi.ruff.lsp.intellij.supports.RuffLspDiagnosticsSupport
import kotlin.io.path.createTempFile
import org.eclipse.lsp4j.DiagnosticRegistrationOptions
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.WorkspaceFolder
import org.eclipse.lsp4j.jsonrpc.messages.Either

class RuffLspServerSupportProviderTest : BasePlatformTestCase() {
    fun testSupportsPythonStubFiles() {
        val executable = createTempFile("ruff", ".tmp").toFile().apply { deleteOnExit() }
        val descriptor = TestRuffLspServerDescriptor(
            project,
            executable,
            RuffConfigService.getInstance(project)
        )

        val stubFile = myFixture.configureByText("example.pyi", "value: int\n").virtualFile

        assertTrue(descriptor.isSupportedFile(stubFile))
    }

    fun testResolvesWslLspFileUri() {
        assertEquals(
            "file://wsl.localhost/Ubuntu/home/test/project",
            resolveWslLspFileUri("""\\wsl.localhost\Ubuntu\home\test\project""", "file:///fallback")
        )
    }

    fun testResolvesWslLspLocalPath() {
        assertEquals(
            "//wsl.localhost/Ubuntu/home/test/project",
            resolveWslLspLocalPath("file://wsl.localhost/Ubuntu/home/test/project")
        )
    }

    fun testNormalizeWslInitializeParamsKeepsUrisAndConvertsRootPath() {
        val params = InitializeParams().apply {
            rootUri = "file://wsl.localhost/Ubuntu/home/test/project"
            rootPath = "//wsl.localhost/Ubuntu/home/test/project"
            workspaceFolders = listOf(
                WorkspaceFolder("file://wsl.localhost/Ubuntu/home/test/project", "project")
            )
        }

        normalizeWslInitializeParams(params)

        assertEquals("""\\wsl.localhost\Ubuntu\home\test\project""", params.rootPath)
        assertEquals("file://wsl.localhost/Ubuntu/home/test/project", params.rootUri)
        assertEquals("file://wsl.localhost/Ubuntu/home/test/project", params.workspaceFolders.single().uri)
    }

    fun testFormatsInitializeParamsLog() {
        val params = InitializeParams().apply {
            rootUri = "file://wsl.localhost/Ubuntu/home/test/project"
            rootPath = """\\wsl.localhost\Ubuntu\home\test\project"""
            workspaceFolders = listOf(
                WorkspaceFolder("file://wsl.localhost/Ubuntu/home/test/project", "project")
            )
        }

        assertEquals(
            "LSP initialize params: rootUri=file://wsl.localhost/Ubuntu/home/test/project, rootPath=\\\\wsl.localhost\\Ubuntu\\home\\test\\project, workspaceFolders=[{name=project, uri=file://wsl.localhost/Ubuntu/home/test/project}]",
            formatInitializeParamsLog(params)
        )
    }

    fun testFormatsInitializeResultLog() {
        val result = InitializeResult(
            ServerCapabilities().apply {
                diagnosticProvider = DiagnosticRegistrationOptions()
                hoverProvider = Either.forLeft(true)
                codeActionProvider = Either.forLeft(true)
                documentFormattingProvider = Either.forLeft(true)
                documentRangeFormattingProvider = Either.forLeft(true)
            }
        )

        assertEquals(
            "LSP initialize result: diagnosticProvider=true, textDocumentSync=null, hoverProvider=true, codeActionProvider=true, documentFormattingProvider=true, documentRangeFormattingProvider=true",
            formatInitializeResultLog(result)
        )
    }

    fun testFormatsPublishDiagnosticsLog() {
        val params = PublishDiagnosticsParams().apply {
            uri = "file://wsl.localhost/Ubuntu/home/test/project/main.py"
            version = 3
            diagnostics = emptyList()
        }

        assertEquals(
            "LSP publish diagnostics: uri=file://wsl.localhost/Ubuntu/home/test/project/main.py, count=0, version=3",
            formatPublishDiagnosticsLog(params)
        )
    }

    fun testWindowsWslLspUsesWslDocumentUriAndPullDiagnostics() {
        if (!SystemInfo.isWindows) return

        val executable = createTempFile("ruff", ".tmp").toFile().apply { deleteOnExit() }
        val descriptor = TestRuffLspServerDescriptor(
            project,
            executable,
            RuffConfigService.getInstance(project)
        )
        val file = WslLightVirtualFile("//wsl.localhost/Ubuntu/home/test/project/main.py")

        assertEquals(
            "file://wsl.localhost/Ubuntu/home/test/project/main.py",
            descriptor.getFileUri(file)
        )

        val diagnosticsSupport = descriptor.lspDiagnosticsSupport as RuffLspDiagnosticsSupport
        assertTrue(diagnosticsSupport.shouldAskServerForDiagnostics(file))
    }

    private class TestRuffLspServerDescriptor(
        project: com.intellij.openapi.project.Project,
        executable: java.io.File,
        ruffConfig: RuffConfigService
    ) : RuffLspServerDescriptorBase(project, executable, ruffConfig) {
        override val logName: String = "test"
        override fun createCommandLine(): GeneralCommandLine = GeneralCommandLine()
    }

    private class WslLightVirtualFile(private val wslPath: String) :
        LightVirtualFile(wslPath.substringAfterLast('/'), "") {
        override fun getPath(): String = wslPath
        override fun getPresentableUrl(): String = wslPath
    }
}
