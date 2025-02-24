package com.koxudaxi.ruff

import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service(Service.Level.PROJECT)
class RuffCacheService(val project: Project) {
    private val lock = ReentrantLock()

    private var version: RuffVersion? = null
    private var hasFormatter: Boolean? = null
    private var hasOutputFormat: Boolean? = null
    private var hasCheck: Boolean? = null
    private var hasLsp: Boolean? = null
    private var hasStableServer: Boolean? = null
    private var projectRuffExecutablePath: String? = null
    private var projectRuffLspExecutablePath: String? = null

    private val versionFutureRef = AtomicReference<CompletableFuture<RuffVersion?>>()

    private inline fun setVersionFromCommand(crossinline action: () -> Unit) {
        val projectRef = project
        executeOnPooledThread {
            if (projectRef.isDisposed) return@executeOnPooledThread
            val fetchedVersion = fetchVersionFromCommand()
            setVersion(fetchedVersion)
            if (fetchedVersion != null && !projectRef.isDisposed) {
                action()
            } else {
                RuffLoggingService.log(project, "Failed to fetch Ruff version, skipping post-version actions")
            }
        }
    }

    private fun getOrPutVersionFromVersionCache(version: String): RuffVersion? {
        return ruffVersionCache.getOrPut(version) {
            try {
                val versionList =
                    version.trimEnd().split(" ").lastOrNull()?.split(".")?.map { it.toIntOrNull() ?: 0 } ?: return null
                when {
                    versionList.size == 1 -> RuffVersion(versionList[0])
                    versionList.size == 2 -> RuffVersion(versionList[0], versionList[1])
                    versionList.size >= 3 -> RuffVersion(versionList[0], versionList[1], versionList[2])
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun setVersion(newVersion: RuffVersion?) {
        lock.withLock {
            version = newVersion
            hasFormatter = newVersion?.hasFormatter
            hasOutputFormat = newVersion?.hasOutputFormat
            hasCheck = newVersion?.hasCheck
            hasLsp = newVersion?.hasLsp
            hasStableServer = newVersion?.hasStableServer
        }
    }

    internal fun clearVersion() {
        setVersion(null)
        versionFutureRef.set(null)
    }

    internal inline fun setVersion(crossinline action: () -> Unit) {
        setVersionFromCommand(action)
    }

    fun getOrPutVersion(): CompletableFuture<RuffVersion?> {
        lock.withLock {
            version?.let { return CompletableFuture.completedFuture(it) }
            versionFutureRef.get()?.let {
                if (!it.isDone && !it.isCancelled && !it.isCompletedExceptionally) {
                    return it
                }
            }
        }

        val existingFuture = versionFutureRef.get()
        if (existingFuture != null && !existingFuture.isDone && !existingFuture.isCancelled && !existingFuture.isCompletedExceptionally) {
            return existingFuture
        }

        val executor = Executor { runnable ->
            getApplication().executeOnPooledThread(runnable)
        }

        val projectRef = project
        val newFuture = CompletableFuture.supplyAsync({
            if (projectRef.isDisposed) return@supplyAsync null
            val newVersion = fetchVersionFromCommand()
            if (!projectRef.isDisposed) {
                lock.withLock {
                    version = newVersion
                }
            }
            newVersion
        }, executor)

        if (!versionFutureRef.compareAndSet(existingFuture, newFuture)) {
            val currentFuture = versionFutureRef.get()
            if (currentFuture != null && !currentFuture.isDone && !currentFuture.isCancelled && !currentFuture.isCompletedExceptionally) {
                return currentFuture
            }
            return newFuture
        }

        return newFuture
    }

    private fun fetchVersionFromCommand(): RuffVersion? {
        return try {
            if (project.isDisposed) return null
            runRuff(project, listOf("--version"), true)
                ?.let { getOrPutVersionFromVersionCache(it) }
        } catch (e: Exception) {
            null
        }
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

    internal fun hasStableServer(): Boolean? {
        return hasStableServer
    }

    internal fun getProjectRuffExecutablePath(): String? {
        return projectRuffExecutablePath
    }

    internal fun setProjectRuffExecutablePath(path: String?) {
        lock.withLock {
            projectRuffExecutablePath = path
        }
    }

    internal fun getProjectRuffLspExecutablePath(): String? {
        return projectRuffLspExecutablePath
    }

    internal fun setProjectRuffLspExecutablePath(path: String?) {
        lock.withLock {
            projectRuffLspExecutablePath = path
        }
    }

    internal companion object {
        fun hasFormatter(project: Project): Boolean = getInstance(project).hasFormatter()

        fun hasOutputFormat(project: Project): Boolean? = getInstance(project).hasOutputFormat()

        fun hasCheck(project: Project): Boolean? = getInstance(project).hasCheck()

        fun hasLsp(project: Project): Boolean? = getInstance(project).hasLsp()

        fun hasStableServer(project: Project): Boolean? = getInstance(project).hasStableServer()

        internal fun getInstance(project: Project): RuffCacheService {
            return project.getService(RuffCacheService::class.java)
        }
    }
}