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
    private fun diffRange(source: String, target: String): TextRange? {
        return postFormatProcessor.diffRange(source, target)
    }

    private fun doTestDiffRange(source: String, target: String, expectTextRange: TextRange, expectInserted: String) {
        val sourceDiffRange = diffRange(source, target)
        assertNotNull(sourceDiffRange)
        assertEquals(expectTextRange, sourceDiffRange)
        val targetDiffRange = diffRange(target, source)
        assertNotNull(targetDiffRange)
        val inserted = target.substring(targetDiffRange.startOffset, targetDiffRange.endOffset)
        assertEquals(expectInserted, targetDiffRange.substring(target))
        assertEquals(target, sourceDiffRange.replace(source, inserted))
    }

    @Test
    fun testDiffRange() {
        assertNull(diffRange("hello", "hello")) // no difference
        doTestDiffRange("hello", "", TextRange(0, 5), "") // complete difference
        doTestDiffRange("hello", "hell", TextRange(4, 5), "")  // difference at the end
        doTestDiffRange("hell", "hello", TextRange(4, 4), "o")  // difference at the end
        doTestDiffRange("hello", "jello", TextRange(0, 1), "j")  // difference at the start
        doTestDiffRange("hello world", "hello world!", TextRange(11, 11), "!")  // difference at the end
        doTestDiffRange("hello world!", "hello world", TextRange(11, 12), "")  // difference at the end
        doTestDiffRange("hello world", "Hello world", TextRange(0, 1), "H")  // difference at the start
        doTestDiffRange("hello world", "hEllo world", TextRange(1, 2), "E")  // difference in the middle
        doTestDiffRange("import a\nimport b\nimport c\n", "import a\nimport c\n", TextRange(16, 25), "")
        doTestDiffRange(
            "import a\nimport c\n",
            "import a\nimport b\nimport c\n",
            TextRange(16, 16),
            "b\nimport "
        )  // difference in the middle
    }
}