package com.koxudaxi.ruff

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.packaging.PyPackageManager

class RuffPackageManagerListener(project: Project) : PyPackageManager.Listener {
    private val ruffConfigService = RuffConfigService.getInstance(project)
    override fun packagesRefreshed(sdk: Sdk) {
        ruffConfigService.projectRuffExecutablePath = findRuffExecutableInSDK(sdk, false)?.absolutePath
    }
}