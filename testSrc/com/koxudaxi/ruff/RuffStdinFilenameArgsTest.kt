package com.koxudaxi.ruff

import kotlin.test.Test
import kotlin.test.assertEquals

class RuffStdinFilenameArgsTest {
    @Test
    fun `uses WSL path when Ruff runs inside WSL`() {
        assertEquals(
            "/home/test/project/main.py",
            resolveStdinFileName(
                "//wsl.localhost/Ubuntu/home/test/project/main.py",
                "main.py",
                true,
                "/home/test/project/main.py"
            )
        )
    }

    @Test
    fun `falls back to project relative path when WSL path resolution fails`() {
        assertEquals(
            "main.py",
            resolveStdinFileName(
                "//wsl.localhost/Ubuntu/home/test/project/main.py",
                "main.py",
                true,
                null
            )
        )
    }

    @Test
    fun `uses UNC path when windows Ruff handles a local WSL project`() {
        assertEquals(
            """\\wsl.localhost\Ubuntu\home\test\project\main.py""",
            resolveStdinFileName(
                "//wsl.localhost/Ubuntu/home/test/project/main.py",
                "main.py",
                false,
                "/home/test/project/main.py"
            )
        )
    }

    @Test
    fun `keeps project relative paths for non WSL files`() {
        assertEquals(
            "src/main.py",
            resolveStdinFileName(
                "/Users/test/project/src/main.py",
                "src/main.py",
                false,
                null
            )
        )
    }
}
