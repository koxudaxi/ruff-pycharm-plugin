import com.intellij.execution.target.TargetBasedSdkAdditionalData
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.target.WslTargetEnvironmentConfiguration
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.koxudaxi.ruff.getWslConfiguration
import com.koxudaxi.ruff.getWslStdinPath
import java.nio.file.Path

class RuffCompatibilityTest : BasePlatformTestCase() {
    fun testReadsWslConfigurationFromTargetBasedSdkData() {
        assertNull(getWslConfiguration(null))
        val localConfiguration = object : TargetEnvironmentConfiguration("local") {
            override var projectRootOnTarget: String = ""
        }
        assertNull(getWslConfiguration(targetData(localConfiguration)))

        val wslConfiguration = WslTargetEnvironmentConfiguration()
        assertSame(wslConfiguration, getWslConfiguration(targetData(wslConfiguration)))
    }

    fun testUsesCanonicalPathWhenWslPathIsUnavailable() {
        val file = myFixture.addFileToProject("pkg/example.py", "value = 1\n")
        val canonicalPath = file.virtualFile.canonicalPath

        assertEquals(
            canonicalPath,
            getWslStdinPath(WslTargetEnvironmentConfiguration(), file.virtualFile)
        )
        val failingDistribution = object : WSLDistribution("test") {
            override fun getWslPath(path: Path): String = throw IllegalStateException("unavailable")
        }
        assertEquals(
            canonicalPath,
            getWslStdinPath(WslTargetEnvironmentConfiguration(failingDistribution), file.virtualFile)
        )
    }

    private fun targetData(configuration: TargetEnvironmentConfiguration): TargetBasedSdkAdditionalData =
        object : TargetBasedSdkAdditionalData {
            override val targetEnvironmentConfiguration: TargetEnvironmentConfiguration = configuration
        }
}
