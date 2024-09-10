import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
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

    @Volatile
    private var enabledClient: RuffLspClient? = null

    fun hasClient(): Boolean {
        return enabledClient != null
    }

    fun setClient(clientType: ClientType, start: Boolean = true) {
        if (clients == null) return
        ApplicationManager.getApplication().invokeLater {
            val newClient = clients[clientType] ?: error("Client for key $clientType not found")
            ApplicationManager.getApplication().runWriteAction {
                if (enabledClient?.getClientType() == clientType) {
                    return@runWriteAction
                }
                enabledClient?.stop()
                if (start) {
                    newClient.start()
                }
                enabledClient = newClient
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