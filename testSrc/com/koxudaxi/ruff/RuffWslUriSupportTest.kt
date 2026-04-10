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
    fun `ignores non WSL paths`() {
        assertNull(buildWslFileUri("/tmp/project"))
        assertNull(buildWslUncPath("file:///tmp/project"))
        assertNull(toWindowsWslUncPath("/tmp/project"))
    }
}
