import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.koxudaxi.ruff.RuffConfigService
import com.koxudaxi.ruff.lsp.intellij.RuffLspServerDescriptorBase
import com.koxudaxi.ruff.lsp.lsp4ij.RuffLanguageServer
import com.koxudaxi.ruff.lsp.lsp4ij.RuffLsp4IntellijClient
import com.koxudaxi.ruff.lsp.lsp4ij.features.RuffLSPCodeActionFeature
import com.koxudaxi.ruff.lsp.lsp4ij.features.RuffLSPDiagnosticFeature
import com.koxudaxi.ruff.lsp.lsp4ij.features.RuffLSPFormattingFeature
import com.koxudaxi.ruff.lsp.lsp4ij.features.RuffLSPHoverFeature
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.LanguageServersRegistry
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
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

    @Suppress("UnstableApiUsage")
    fun testRegistersLsp4ijServerForProject() {
        val definition = kotlin.test.assertNotNull(
            LanguageServersRegistry.getInstance()
                .getServerDefinition(RuffLsp4IntellijClient.LANGUAGE_SERVER_ID)
        )
        val factory: LanguageServerFactory = definition
        val client = factory.createLanguageClient(project)
        try {
            assertSame(LanguageClientImpl::class.java, client.javaClass)
            assertSame(project, client.project)
        } finally {
            Disposer.dispose(client)
        }
        assertTrue(factory.createConnectionProvider(project) is RuffLanguageServer)
        val features = factory.createClientFeatures()
        try {
            assertTrue(features.codeActionFeature is RuffLSPCodeActionFeature)
            assertTrue(features.diagnosticFeature is RuffLSPDiagnosticFeature)
            assertTrue(features.formattingFeature is RuffLSPFormattingFeature)
            assertTrue(features.hoverFeature is RuffLSPHoverFeature)
        } finally {
            Disposer.dispose(features)
        }
    }

    private class TestRuffLspServerDescriptor(
        project: com.intellij.openapi.project.Project,
        executable: java.io.File,
        ruffConfig: RuffConfigService
    ) : RuffLspServerDescriptorBase(project, executable, ruffConfig) {
        override fun createCommandLine(): GeneralCommandLine = GeneralCommandLine()
    }
}
