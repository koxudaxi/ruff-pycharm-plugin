import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.koxudaxi.ruff.RuffConfigService
import com.koxudaxi.ruff.lsp.intellij.RuffLspServerDescriptorBase
import kotlin.io.path.createTempFile

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

    private class TestRuffLspServerDescriptor(
        project: com.intellij.openapi.project.Project,
        executable: java.io.File,
        ruffConfig: RuffConfigService
    ) : RuffLspServerDescriptorBase(project, executable, ruffConfig) {
        override fun createCommandLine(): GeneralCommandLine = GeneralCommandLine()
    }
}
