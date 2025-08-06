import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.koxudaxi.ruff.RuffLoggingService
import com.koxudaxi.ruff.intellijLspClientSupported
import com.koxudaxi.ruff.lsp.ClientType
import com.koxudaxi.ruff.lsp.RuffLspClient
import com.koxudaxi.ruff.lsp.intellij.RuffIntellijLspClient
import com.koxudaxi.ruff.lsp.lsp4ij.RuffLsp4IntellijClient
import com.koxudaxi.ruff.lsp4ijSupported

@Service(Service.Level.PROJECT)
class RuffLspClientManager(project: Project) {
    private val clients: Map<ClientType, RuffLspClient>? = when {
        project.isDefault -> null
        else -> listOfNotNull(
            if (lsp4ijSupported) ClientType.LSP4IJ to RuffLsp4IntellijClient(project) else null,
            if (intellijLspClientSupported) ClientType.INTELLIJ to RuffIntellijLspClient(project) else null
        ).toMap().ifEmpty { null }
    }
    fun getClient(clientType: ClientType): RuffLspClient? {
        return clients?.get(clientType)
    }
    @Volatile
    private var enabledClient: RuffLspClient? = null

    init {
        project.messageBus.connect().subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
            override fun projectClosing(project: Project) {
                RuffLoggingService.log(project, "Stopping LSP clients due to project closing")
                stop()
            }
        })
    }

    fun hasClient(): Boolean {
        return enabledClient != null
    }

    fun setClient(clientType: ClientType, start: Boolean = true) {
        if (clients == null) return
        if (clients[clientType] == null) return

        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                val currentClient = clients[clientType] ?: return@runWriteAction

                if (enabledClient?.getClientType() == clientType) {
                    if (start) {
                        enabledClient?.restart()
                    }
                    return@runWriteAction
                }
                enabledClient?.stop()
                if (start) {
                    currentClient.start()
                }
                enabledClient = currentClient
            }
        }
    }

    fun start() {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                enabledClient?.start()
            }
        }
    }

    fun stop() {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                enabledClient?.stop()
                enabledClient = null
            }
        }
    }

    fun restart() {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                enabledClient?.restart()
            }
        }
    }

    companion object {
        fun getInstance(project: Project): RuffLspClientManager {
            return project.getService(RuffLspClientManager::class.java)
        }
    }
}