package com.koxudaxi.ruff

import com.intellij.codeInspection.ex.ExternalAnnotatorBatchInspection
import com.jetbrains.python.inspections.PyInspection

class RuffInspection : PyInspection(), ExternalAnnotatorBatchInspection {
    override fun getShortName(): String = INSPECTION_SHORT_NAME

    companion object {
        const val INSPECTION_SHORT_NAME = "RuffInspection"
    }
}
