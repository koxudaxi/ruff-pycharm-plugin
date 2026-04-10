package com.koxudaxi.ruff

import kotlin.test.Test
import kotlin.test.assertEquals

class RuffFormatArgsTest {
    @Test
    fun buildFormatArgsIncludesStdinFilename() {
        assertEquals(
            listOf("format", "--force-exclude", "--quiet", "--stdin-filename", "nested/example.py"),
            buildFormatArgs(stdinFileNameArgs = listOf("--stdin-filename", "nested/example.py"))
        )
    }

    @Test
    fun buildFormatArgsPreservesExtraArgsBeforeStdinFilename() {
        assertEquals(
            listOf("format", "--force-exclude", "--quiet", "--range", "1:1-1:5", "--stdin-filename", "example.py"),
            buildFormatArgs(listOf("--range", "1:1-1:5"), listOf("--stdin-filename", "example.py"))
        )
    }
}
