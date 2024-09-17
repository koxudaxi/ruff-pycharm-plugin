package com.koxudaxi.ruff

import com.intellij.openapi.project.Project


class RuffAsyncFixFormatter : RuffAsyncFormatterBase() {
    override fun isEnabled(project: Project): Boolean =
        RuffConfigService.getInstance(project).runRuffOnReformatCode

    override fun getArgs(project: Project): List<String> = project.FIX_ARGS
    override fun getName(): String = "Ruff Fix Formatter"
}