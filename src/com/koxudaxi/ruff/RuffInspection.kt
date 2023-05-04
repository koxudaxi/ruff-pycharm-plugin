package com.koxudaxi.ruff

import com.intellij.codeInspection.ex.ExternalAnnotatorBatchInspection
import com.intellij.codeInspection.options.OptPane
import com.jetbrains.python.PyBundle
import com.jetbrains.python.inspections.PyInspection

class RuffInspection : PyInspection(), ExternalAnnotatorBatchInspection {
    var ignoredErrors: List<String> = ArrayList()
    override fun getOptionsPane(): OptPane {
        return OptPane.pane(
        )
    }

    override fun getShortName(): String {
        return INSPECTION_SHORT_NAME
    }

    companion object {
        const val INSPECTION_SHORT_NAME = "RuffInspection"
    }
}
