import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.koxudaxi.ruff.lsp.RuffLspClient
import com.koxudaxi.ruff.lsp.intellij.RuffIntellijLspClient
import com.koxudaxi.ruff.lsp.lsp4ij.RuffLsp4IntellijClient
import kotlin.reflect.KClass

@Service(Service.Level.PROJECT)
class RuffLspClientManager(project: Project) {

    private val clients: Map<KClass<out RuffLspClient>, RuffLspClient>? = when {
        project.isDefault -> null
        else -> mapOf(
            RuffLsp4IntellijClient::class to RuffLsp4IntellijClient(project),
            RuffIntellijLspClient::class to RuffIntellijLspClient(project)
        )
    }

    @Volatile
    private var enabledClient: RuffLspClient? = null

    fun hasClient(): Boolean {
        return enabledClient != null
    }

    fun setClient(client: KClass<out RuffLspClient>, start: Boolean = true) {
        if (clients == null) return
        ApplicationManager.getApplication().invokeLater {
            val newClient = clients[client] ?: error("Client for key $client not found")
            ApplicationManager.getApplication().runWriteAction {
                if (enabledClient?.javaClass == client.java) {
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