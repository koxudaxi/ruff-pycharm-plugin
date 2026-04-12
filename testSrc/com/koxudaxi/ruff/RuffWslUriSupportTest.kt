package com.koxudaxi.ruff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RuffWslUriSupportTest {
    @Test
    fun `builds a WSL file URI from a system independent UNC path`() {
        assertEquals(
            "file://wsl.localhost/Ubuntu/home/test/project",
            buildWslFileUri("//wsl.localhost/Ubuntu/home/test/project")
        )
    }

    @Test
    fun `builds a WSL file URI from a windows UNC path`() {
        assertEquals(
            "file://wsl.localhost/Ubuntu/home/test/project",
            buildWslFileUri("""\\wsl.localhost\Ubuntu\home\test\project""")
        )
    }

    @Test
    fun `converts a WSL file URI back to a UNC path`() {
        assertEquals(
            "//wsl.localhost/Ubuntu/home/test/project",
            buildWslUncPath("file://wsl.localhost/Ubuntu/home/test/project")
        )
    }

    @Test
    fun `converts an authority-less WSL file URI back to a UNC path`() {
        assertEquals(
            "//wsl.localhost/Ubuntu/home/test/project",
            buildWslUncPath("file:///wsl.localhost/Ubuntu/home/test/project")
        )
    }

    @Test
    fun `converts a system independent WSL path to a windows UNC path`() {
        assertEquals(
            """\\wsl.localhost\Ubuntu\home\test\project""",
            toWindowsWslUncPath("//wsl.localhost/Ubuntu/home/test/project")
        )
    }

    @Test
    fun `builds and restores URI for wsl dollar host`() {
        val uncPath = """\\wsl$\Ubuntu\home\test\project"""
        val fileUri = "file://wsl$/Ubuntu/home/test/project"
        assertEquals(fileUri, buildWslFileUri(uncPath))
        assertEquals("//wsl$/Ubuntu/home/test/project", buildWslUncPath(fileUri))
        assertEquals(uncPath, toWindowsWslUncPath("//wsl$/Ubuntu/home/test/project"))
    }

    @Test
    fun `escapes URI segments and decodes them when converting back`() {
        val uncPath = """\\wsl.localhost\Ubuntu\home\test\My Project"""
        val fileUri = "file://wsl.localhost/Ubuntu/home/test/My%20Project"
        assertEquals(fileUri, buildWslFileUri(uncPath))
        assertEquals("//wsl.localhost/Ubuntu/home/test/My Project", buildWslUncPath(fileUri))
    }

    @Test
    fun `ignores non WSL paths`() {
        assertNull(buildWslFileUri("/tmp/project"))
        assertNull(buildWslUncPath("file:///tmp/project"))
        assertNull(toWindowsWslUncPath("/tmp/project"))
    }
}
