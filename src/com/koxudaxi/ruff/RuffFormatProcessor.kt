package com.koxudaxi.ruff

import com.intellij.openapi.project.Project


class RuffFormatProcessor : RuffPostFormatProcessor() {
    override fun isEnabled(project: Project): Boolean {
        val ruffConfigService = RuffConfigService.getInstance(project)
        return ruffConfigService.runRuffOnReformatCode && ruffConfigService.useRuffFormat && RuffCacheService.hasFormatter(project)
    }
    override fun process(sourceFile: SourceFile): String? =  format(sourceFile)
}