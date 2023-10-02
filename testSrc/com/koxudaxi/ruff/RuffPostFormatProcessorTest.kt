package com.koxudaxi.ruff

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


class RuffPostFormatProcessorTest {
    class RuffPostFormatProcessorBase : RuffPostFormatProcessor() {
        override fun isEnabled(project: Project): Boolean {
            TODO("Not yet implemented")
        }

        override fun process(sourceFile: SourceFile): String? {
            TODO("Not yet implemented")
        }
    }

    private val postFormatProcessor = RuffPostFormatProcessorBase()
    private fun diffRange(s1: String, s2: String): TextRange? {
        return postFormatProcessor.diffRange(s1, s2)
    }
    private fun testDiffRange(textRange: TextRange, s1: String, s2: String, expect: String) {
        val diffTextRange = diffRange(s1, s2)
        assertEquals(textRange, diffTextRange)
        assertNotNull(diffTextRange)
        val diff = diffTextRange.substring(s1)
        assertEquals(expect, diff)
    }

    @Test
    fun diffRange() {
        assertNull(diffRange("hello", "hello")) // no difference
        testDiffRange(TextRange(0, 5), "hello", "", "hello") // complete difference
        testDiffRange(TextRange(0, 0),"", "hello", "") // complete difference
        testDiffRange(TextRange(4, 5),"hello", "hell", "o") // end difference
        testDiffRange(TextRange(0, 1), "hello", "eello", "h") // start difference
        testDiffRange(TextRange(3, 4), "hello", "heleo", "l") // middle difference
        testDiffRange(TextRange(4, 5),"hello", "hellx", "o")
        testDiffRange(TextRange(0, 5),"hello", "abcd", "hello")

    }
}