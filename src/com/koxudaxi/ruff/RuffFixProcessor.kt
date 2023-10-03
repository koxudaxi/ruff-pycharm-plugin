package com.koxudaxi.ruff

import com.intellij.openapi.project.Project


class RuffFixProcessor : RuffPostFormatProcessor() {
    override fun isEnabled(project: Project): Boolean =
        RuffConfigService.getInstance(project).runRuffOnReformatCode

    override fun process(sourceFile: SourceFile): String? = fix(sourceFile)
}