import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerManager
import com.koxudaxi.ruff.RuffLoggingService
import com.koxudaxi.ruff.configService
import com.koxudaxi.ruff.intellijLspClientSupported
import com.koxudaxi.ruff.lsp.ClientType
import com.koxudaxi.ruff.lsp.RuffLspClient
import com.koxudaxi.ruff.lsp.intellij.RuffIntellijLspClient
import com.koxudaxi.ruff.lsp.lsp4ij.RuffLsp4IntellijClient
import com.koxudaxi.ruff.lsp4ijSupported
import com.redhat.devtools.lsp4ij.LanguageServerManager
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Suppress("UnstableApiUsage")
@Service(Service.Level.PROJECT)
class RuffLspClientManager(val project: Project) {
    private val clients: Map<ClientType, RuffLspClient>? = when {
        project.isDefault -> null
        else -> listOfNotNull(
            if (lsp4ijSupported) ClientType.LSP4IJ to RuffLsp4IntellijClient(project) else null,
            if (intellijLspClientSupported) ClientType.INTELLIJ to RuffIntellijLspClient(project) else null
        ).toMap().ifEmpty { null }
    }

    @Volatile
    private var enabledClient: RuffLspClient? = null
    private val clientLock = ReentrantLock()
    private val switching = AtomicBoolean(false)

    fun hasClient(): Boolean {
        return enabledClient != null
    }

    fun setClient(clientType: ClientType, start: Boolean = true) {
        if (clients == null) return
        if (clients[clientType] == null) return

        if (!switching.compareAndSet(false, true)) {
            RuffLoggingService.log(project, "Client switch already in progress, ignoring request", ConsoleViewContentType.NORMAL_OUTPUT)
            return
        }

        try {
            ApplicationManager.getApplication().invokeLater {
                try {
                    ApplicationManager.getApplication().runWriteAction {
                        try {
                            clientLock.withLock {
                                val currentClient = clients[clientType] ?: return@runWriteAction

                                RuffLoggingService.log(project, "Setting LSP client to ${clientType} (start=$start)", ConsoleViewContentType.NORMAL_OUTPUT)

                                if (enabledClient?.getClientType() == clientType) {
                                    if (start) {
                                        RuffLoggingService.log(project, "Client type unchanged, restarting", ConsoleViewContentType.NORMAL_OUTPUT)
                                        enabledClient?.restart()
                                    }
                                    return@withLock
                                }

                                val ruffConfigService = project.configService
                                if (clientType == ClientType.LSP4IJ) {
                                    ruffConfigService.useLsp4ij = true
                                    ruffConfigService.useIntellijLspClient = false
                                } else {
                                    ruffConfigService.useLsp4ij = false
                                    ruffConfigService.useIntellijLspClient = true
                                }

                                val previousClient = enabledClient
                                enabledClient = null  // Set to null first to avoid callbacks

                                if (clientType == ClientType.LSP4IJ) {
                                    try {
                                        RuffLoggingService.log(project, "Explicitly stopping IntelliJ LSP client", ConsoleViewContentType.NORMAL_OUTPUT)
                                        LspServerManager.getInstance(project)
                                            .stopServers(RuffLspServerSupportProvider::class.java)
                                    } catch (e: Exception) {
                                        RuffLoggingService.log(project, "Error stopping IntelliJ LSP client: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
                                    }
                                } else {
                                    try {
                                        RuffLoggingService.log(project, "Explicitly stopping LSP4IJ client", ConsoleViewContentType.NORMAL_OUTPUT)
                                        val languageServerManager = LanguageServerManager.getInstance(project)
                                        languageServerManager.stop(RuffLsp4IntellijClient.LANGUAGE_SERVER_ID,
                                            LanguageServerManager.StopOptions().setWillDisable(true))
                                    } catch (e: Exception) {
                                        RuffLoggingService.log(project, "Error stopping LSP4IJ client: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
                                    }
                                }

                                // Now stop the previous client
                                try {
                                    previousClient?.stop()
                                } catch (e: Exception) {
                                    RuffLoggingService.log(project, "Error stopping previous client: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
                                }

                                // Start the new client with a delay to let previous client clean up
                                if (start) {
                                    RuffLoggingService.log(project, "Starting new ${clientType} client", ConsoleViewContentType.NORMAL_OUTPUT)
                                    enabledClient = currentClient

                                    ApplicationManager.getApplication().executeOnPooledThread {
                                        try {
                                            // A brief pause is necessary to ensure clean shutdown
                                            Thread.sleep(300)
                                            currentClient.start()
                                        } catch (e: Exception) {
                                            RuffLoggingService.log(project,
                                                "Error starting ${clientType} client: ${e.message}",
                                                ConsoleViewContentType.ERROR_OUTPUT)
                                        }
                                    }
                                } else {
                                    enabledClient = currentClient
                                }
                            }
                        } catch (e: Exception) {
                            RuffLoggingService.log(project, "Error during client switch: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
                        }
                    }
                } catch (e: Exception) {
                    RuffLoggingService.log(project, "Error in runWriteAction during client switch: ${e.message}", ConsoleViewContentType.ERROR_OUTPUT)
                }
            }
        } finally {
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    Thread.sleep(1000)
                } finally {
                    switching.set(false)
                }
            }
        }
    }

    fun start() {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                clientLock.withLock {
                    enabledClient?.start()
                }
            }
        }
    }

    fun stop() {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                clientLock.withLock {
                    val client = enabledClient
                    enabledClient = null
                    client?.stop()
                }
            }
        }
    }

    fun restart() {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                clientLock.withLock {
                    enabledClient?.restart()
                }
            }
        }
    }

    companion object {
        fun getInstance(project: Project): RuffLspClientManager {
            return project.getService(RuffLspClientManager::class.java)
        }
    }
}