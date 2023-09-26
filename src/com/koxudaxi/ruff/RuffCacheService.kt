package com.koxudaxi.ruff

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class RuffCacheService(val project: Project) {
    private var version: RuffVersion? = null
    private var hasFormatter: Boolean? = null
    private var hasOutputFormat: Boolean? = null

    fun getVersion(): RuffVersion? {
        return version
    }
    private fun getVersionFromCommand(): RuffVersion? =
        // TODO: Move to background task to avoid blocking UI thread
        executeOnPooledThread(null) {
            runRuff(project, listOf("--version"), true)
        }?.let { getOrPutVersionFromVersionCache(it) }


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

    private fun setVersion(version: RuffVersion?) {
        this.version = version
        hasFormatter = version?.hasFormatter
        hasOutputFormat = version?.hasOutputFormat
    }

    internal fun setVersion(): RuffVersion? {
        return getVersionFromCommand().apply { setVersion(this) }
    }

    internal fun hasFormatter(): Boolean {
        return hasFormatter ?: false
    }

    internal fun hasOutputFormat(): Boolean {
        return hasOutputFormat ?: false
    }

    internal
    companion object {
        fun hasFormatter(project: Project): Boolean = getInstance(project).hasFormatter()

        fun hasOutputFormat(project: Project): Boolean = getInstance(project).hasOutputFormat()

        fun setVersion(project: Project): RuffVersion? {
            return getInstance(project).setVersion()
        }

        internal fun getInstance(project: Project): RuffCacheService {
            return project.getService(RuffCacheService::class.java)
        }
    }
}