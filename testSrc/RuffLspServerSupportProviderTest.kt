import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.koxudaxi.ruff.RuffConfigService
import kotlin.io.path.createTempFile
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.WorkspaceFolder

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

    private class TestRuffLspServerDescriptor(
        project: com.intellij.openapi.project.Project,
        executable: java.io.File,
        ruffConfig: RuffConfigService
    ) : RuffLspServerDescriptorBase(project, executable, ruffConfig) {
        override fun createCommandLine(): GeneralCommandLine = GeneralCommandLine()
    }
}
