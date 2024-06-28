package com.koxudaxi.ruff

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class RuffCacheService(val project: Project) {
    private var version: RuffVersion? = null
    private var hasFormatter: Boolean? = null
    private var hasOutputFormat: Boolean? = null
    private var hasCheck: Boolean? = null
    private var hasLsp: Boolean? = null

    fun getVersion(): RuffVersion? {
        return version
    }
    private inline fun setVersionFromCommand(crossinline action: () -> Unit) =
        executeOnPooledThread {
            val ruffVersion = runRuff(project, listOf("--version"), true)
                ?.let { getOrPutVersionFromVersionCache(it) }
            setVersion(ruffVersion)
            action()
        }


    private fun getOrPutVersionFromVersionCache(version: String): RuffVersion? {
        return ruffVersionCache.getOrPut(version) {
            val versionList =
                version.trimEnd().split(" ").lastOrNull()?.split(".")?.map { it.toIntOrNull() ?: 0 } ?: return null
            val ruffVersion = when {
                versionList.size == 1 -> RuffVersion(versionList[0])
                versionList.size == 2 -> RuffVersion(versionList[0], versionList[1])
                versionList.size >= 3 -> RuffVersion(versionList[0], versionList[1], versionList[2])
                else -> null
            } ?: return null
            ruffVersionCache[version] = ruffVersion
            ruffVersion
        }
    }

    @Synchronized
    private fun setVersion(version: RuffVersion?) {
        this.version = version
        hasFormatter = version?.hasFormatter
        hasOutputFormat = version?.hasOutputFormat
        hasCheck = version?.hasCheck
        hasLsp = version?.hasLsp
    }

    internal fun clearVersion() {
        setVersion(null)
    }

    internal inline fun setVersion(crossinline action: () -> Unit) {
        return setVersionFromCommand(action)
    }

    internal fun hasFormatter(): Boolean {
        return hasFormatter ?: false
    }

    internal fun hasOutputFormat(): Boolean? {
        return hasOutputFormat
    }

    internal fun hasCheck(): Boolean? {
        return hasCheck
    }

    internal fun hasLsp(): Boolean? {
        return hasLsp
    }
    internal
    companion object {
        fun hasFormatter(project: Project): Boolean = getInstance(project).hasFormatter()

        fun hasOutputFormat(project: Project): Boolean? = getInstance(project).hasOutputFormat()

        fun hasCheck(project: Project): Boolean? = getInstance(project).hasCheck()

        fun hasLsp(project: Project): Boolean? = getInstance(project).hasLsp()

        internal fun getInstance(project: Project): RuffCacheService {
            return project.getService(RuffCacheService::class.java)
        }
    }
}