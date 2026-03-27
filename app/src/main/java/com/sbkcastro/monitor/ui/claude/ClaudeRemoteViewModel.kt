package com.sbkcastro.monitor.ui.claude

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.ClaudeJobMessageRequest
import com.sbkcastro.monitor.api.ClaudeJobStartRequest
import com.sbkcastro.monitor.api.ClaudeSession
import com.sbkcastro.monitor.api.MetaChatRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

data class ChatBubble(
    val role: String,           // "user" | "assistant" | "tool" | "system"
    val text: String,
    val toolState: String = ""  // "" | "pending" | "approved" | "denied"
)

class ClaudeRemoteViewModel : ViewModel() {

    private val _sessions = MutableLiveData<List<ClaudeSession>>(emptyList())
    val sessions: LiveData<List<ClaudeSession>> = _sessions

    private val _bubbles = MutableLiveData<List<ChatBubble>>(emptyList())
    val bubbles: LiveData<List<ChatBubble>> = _bubbles

    val streamingText = MutableLiveData<String?>(null)

    private val _pendingApproval = MutableLiveData<Triple<String, String, String>?>(null)
    val pendingApproval: LiveData<Triple<String, String, String>?> = _pendingApproval

    val activeJobId = MutableLiveData<String?>(null)

    // ── Meta-IA mode ──────────────────────────────────────────────────────────
    val metaMode = MutableLiveData(false)
    private var metaSessionId: String? = null
    val metaThinking = MutableLiveData(false)
    val pendingExecute = MutableLiveData<String?>(null)

    // ── Model selection ───────────────────────────────────────────────────────
    companion object {
        val MODELS = listOf(
            "sonnet-4-6"  to "claude-sonnet-4-6",
            "opus-4-6"    to "claude-opus-4-6",
            "haiku-4-5"   to "claude-haiku-4-5-20251001"
        )
    }
    val selectedModelIdx = MutableLiveData(0) // 0 = sonnet (default)
    private val selectedModel get() = MODELS[selectedModelIdx.value ?: 0].second

    fun cycleModel() {
        val next = ((selectedModelIdx.value ?: 0) + 1) % MODELS.size
        selectedModelIdx.value = next
        addBubble(ChatBubble("system", "⬥ Modelo: ${MODELS[next].first}"))
    }

    private var streamJob: Job? = null

    fun loadSessions() {
        viewModelScope.launch {
            try {
                val resp = ApiClient.getService().claudeJobSessions()
                _sessions.value = resp.sessions
            } catch (e: Exception) {
                android.util.Log.e("ClaudeRemote", "loadSessions failed: ${e.message}", e)
            }
        }
    }

    fun startSession(message: String, resumeSessionId: String? = null) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.getService().claudeJobStart(
                    ClaudeJobStartRequest(message, resumeSessionId, selectedModel)
                )
                activeJobId.value = resp.jobId
                addBubble(ChatBubble("user", message))
                connectToJob(resp.jobId)
                loadSessions()
            } catch (e: Exception) {
                addBubble(ChatBubble("system", "❌ Error al iniciar sesión: ${e.message}"))
            }
        }
    }

    fun sendMessage(message: String) {
        val currentJob = activeJobId.value ?: run {
            startSession(message)
            return
        }
        viewModelScope.launch {
            try {
                val resp = ApiClient.getService().claudeJobMessage(
                    currentJob, ClaudeJobMessageRequest(message)
                )
                activeJobId.value = resp.newJobId
                addBubble(ChatBubble("user", message))
                connectToJob(resp.newJobId)
            } catch (e: Exception) {
                addBubble(ChatBubble("system", "❌ Error: ${e.message}"))
            }
        }
    }

    fun connectToJob(jobId: String) {
        streamJob?.cancel()
        streamJob = viewModelScope.launch(Dispatchers.IO) {
            var lastEventId = -1
            var retryMs = 2000L

            while (true) {
                try {
                    val reqBuilder = Request.Builder()
                        .url("${ApiClient.getBaseUrl()}api/claude/job/$jobId/stream")
                    if (lastEventId >= 0) reqBuilder.header("Last-Event-ID", lastEventId.toString())

                    ApiClient.getStreamingClient().newCall(reqBuilder.build()).execute().use { response ->
                        retryMs = 2000L
                        response.body?.source()?.let { source ->
                            var currentId = -1
                            while (!source.exhausted()) {
                                val line = source.readUtf8Line() ?: break
                                when {
                                    line.startsWith("id: ") -> {
                                        currentId = line.removePrefix("id: ").trim().toIntOrNull() ?: currentId
                                        lastEventId = currentId
                                    }
                                    line.startsWith("data: ") -> {
                                        processEvent(jobId, line.removePrefix("data: "))
                                    }
                                }
                            }
                        }
                    }
                    break // stream ended normally
                } catch (e: Exception) {
                    android.util.Log.e("ClaudeRemote", "SSE error (retry in ${retryMs}ms): ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        if (retryMs >= 8_000L) {
                            addBubble(ChatBubble("system", "⚠️ Sin conexión SSE (${e.javaClass.simpleName}). Reintentando..."))
                        }
                    }
                    delay(retryMs)
                    retryMs = minOf(retryMs * 2, 30_000L)
                }
            }
        }
    }

    private suspend fun processEvent(jobId: String, data: String) {
        try {
            val obj = JSONObject(data)
            when (obj.optString("type")) {
                "text" -> {
                    val content = obj.optString("content", "")
                    val current = streamingText.value ?: ""
                    withContext(Dispatchers.Main) { streamingText.value = current + content }
                }
                "tool_use" -> {
                    val tool = obj.optString("tool", "")
                    val input = obj.optJSONObject("input")?.toString() ?: obj.optString("input", "")
                    withContext(Dispatchers.Main) {
                        flushStreaming()
                        addBubble(ChatBubble("tool", "$tool: $input", "pending"))
                    }
                }
                "tool_result" -> {
                    withContext(Dispatchers.Main) { updateLastToolBubble("approved") }
                }
                "approval_needed" -> {
                    val tool = obj.optString("tool")
                    val command = obj.optString("command")
                    withContext(Dispatchers.Main) {
                        _pendingApproval.value = Triple(jobId, tool, command)
                    }
                }
                "approval_resolved" -> {
                    val decision = obj.optString("decision")
                    withContext(Dispatchers.Main) {
                        _pendingApproval.value = null
                        updateLastToolBubble(if (decision == "approve") "approved" else "denied")
                    }
                }
                "done" -> {
                    withContext(Dispatchers.Main) {
                        flushStreaming()
                        _pendingApproval.value = null
                        loadSessions()
                    }
                }
                "error" -> {
                    val msg = obj.optString("message", "Error desconocido")
                    withContext(Dispatchers.Main) {
                        flushStreaming()
                        addBubble(ChatBubble("system", "⚠️ $msg"))
                    }
                }
            }
        } catch (_: Exception) { }
    }

    fun approve() {
        val ap = _pendingApproval.value ?: return
        val (jobId, _, _) = ap
        viewModelScope.launch {
            try {
                ApiClient.getService().claudeJobApprove(jobId)
                withContext(Dispatchers.Main) { _pendingApproval.value = null }
            } catch (_: Exception) { }
        }
    }

    fun deny() {
        val ap = _pendingApproval.value ?: return
        val (jobId, _, _) = ap
        viewModelScope.launch {
            try {
                ApiClient.getService().claudeJobDeny(jobId)
                withContext(Dispatchers.Main) { _pendingApproval.value = null }
            } catch (_: Exception) { }
        }
    }

    fun deleteSession(jobId: String) {
        viewModelScope.launch {
            try {
                ApiClient.getService().claudeJobDelete(jobId)
                loadSessions()
            } catch (_: Exception) { }
        }
    }

    fun resumeSession(session: ClaudeSession) {
        streamJob?.cancel()
        activeJobId.value = null
        _bubbles.value = emptyList()
        streamingText.value = null
        _pendingApproval.value = null
        addBubble(ChatBubble("system", "▶️ Retomando: \"${session.firstMessage}\""))
        startSession("continúa desde donde lo dejaste", session.claudeSessionId)
    }

    fun launchPending(editedPrompt: String? = null) {
        val prompt = editedPrompt ?: pendingExecute.value ?: return
        pendingExecute.value = null
        addBubble(ChatBubble("system", "🚀 Ejecutando en Claude CLI..."))
        startSession(prompt)
    }

    fun cancelPending() {
        pendingExecute.value = null
    }

    fun newSession() {
        streamJob?.cancel()
        activeJobId.value = null
        _bubbles.value = emptyList()
        streamingText.value = null
        _pendingApproval.value = null
        pendingExecute.value = null
        metaSessionId = null
    }

    fun toggleMetaMode() {
        val next = !(metaMode.value ?: false)
        metaMode.value = next
        if (!next) {
            // Al salir de meta mode: limpiar sesión meta
            metaSessionId?.let { sid ->
                viewModelScope.launch {
                    try { ApiClient.getService().metaSessionDelete(sid) } catch (_: Exception) {}
                }
            }
            metaSessionId = null
        }
        addBubble(ChatBubble("system", if (next) "🤖 Meta-IA activada — habla conmigo y ejecutaré Claude cuando esté listo." else "⬥ Modo directo Claude CLI"))
    }

    fun sendMetaMessage(message: String) {
        addBubble(ChatBubble("user", message))
        metaThinking.value = true
        viewModelScope.launch {
            try {
                val resp = ApiClient.getService().metaChat(
                    MetaChatRequest(message, metaSessionId)
                )
                metaSessionId = resp.sessionId
                withContext(Dispatchers.Main) {
                    metaThinking.value = false
                    if (resp.reply.isNotBlank()) {
                        addBubble(ChatBubble("meta", resp.reply))
                    }
                    // Si meta-IA decidió ejecutar → mostrar preview para confirmar
                    if (!resp.executePrompt.isNullOrBlank()) {
                        pendingExecute.value = resp.executePrompt
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    metaThinking.value = false
                    addBubble(ChatBubble("system", "❌ Meta-IA error: ${e.message}"))
                }
            }
        }
    }

    private fun flushStreaming() {
        val text = streamingText.value
        if (!text.isNullOrEmpty()) addBubble(ChatBubble("assistant", text))
        streamingText.value = null
    }

    fun addBubble(bubble: ChatBubble) {
        val list = _bubbles.value?.toMutableList() ?: mutableListOf()
        list.add(bubble)
        _bubbles.value = list
    }

    private fun updateLastToolBubble(newState: String) {
        val list = _bubbles.value?.toMutableList() ?: return
        val idx = list.indexOfLast { it.role == "tool" && it.toolState == "pending" }
        if (idx >= 0) {
            list[idx] = list[idx].copy(toolState = newState)
            _bubbles.value = list
        }
    }
}
