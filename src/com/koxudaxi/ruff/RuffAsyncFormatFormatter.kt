package com.koxudaxi.ruff

import com.intellij.openapi.project.Project

class RuffAsyncFormatFormatter : RuffAsyncFormatterBase() {
    override fun isEnabled(project: Project): Boolean {
        val ruffConfigService = RuffConfigService.getInstance(project)
        return ruffConfigService.runRuffOnReformatCode && ruffConfigService.useRuffFormat
    }

    override fun getArgs(project: Project): List<String> = FORMAT_ARGS
    override fun getName(): String = "Ruff Format Formatter"

}