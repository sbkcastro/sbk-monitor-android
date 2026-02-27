package com.sbkcastro.monitor.ui.chat

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.AuditPreset
import com.sbkcastro.monitor.api.ChatMessage
import com.sbkcastro.monitor.api.ChatSendRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import org.json.JSONObject

class ChatViewModel : ViewModel() {
    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _sending = MutableLiveData<Boolean>()
    val sending: LiveData<Boolean> = _sending

    private val _presets = MutableLiveData<List<AuditPreset>>(emptyList())
    val presets: LiveData<List<AuditPreset>> = _presets

    // Texto parcial del stream en curso (null = no hay stream activo)
    private val _streamingText = MutableLiveData<String?>(null)
    val streamingText: LiveData<String?> = _streamingText

    var currentBackend = "openclaw"
    private var sessionPrefs: SharedPreferences? = null

    fun initPrefs(context: Context) {
        sessionPrefs = context.getSharedPreferences("claude_session", Context.MODE_PRIVATE)
    }

    private fun getSessionId(): String? = sessionPrefs?.getString("session_id", null)

    private fun saveSessionId(id: String) {
        if (id.isNotEmpty()) sessionPrefs?.edit()?.putString("session_id", id)?.apply()
    }

    fun clearSession() {
        sessionPrefs?.edit()?.remove("session_id")?.apply()
        val list = _messages.value?.toMutableList() ?: mutableListOf()
        list.add(ChatMessage("assistant", "✅ Nueva conversación iniciada.", "claude", System.currentTimeMillis()))
        _messages.value = list
    }

    fun loadPresets() {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().getChatPresets()
                _presets.value = response.presets
            } catch (_: Exception) { }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                val history = ApiClient.getService().getChatHistory()
                _messages.value = history.messages
            } catch (_: Exception) { }
        }
    }

    fun sendMessage(text: String) {
        if (currentBackend == "claude") {
            sendMessageClaude(text)
        } else {
            sendMessageOpenClaw(text)
        }
    }

    private fun sendMessageOpenClaw(text: String) {
        _sending.value = true
        val currentList = _messages.value?.toMutableList() ?: mutableListOf()
        currentList.add(ChatMessage("user", text, currentBackend, System.currentTimeMillis()))
        _messages.value = currentList

        viewModelScope.launch {
            try {
                val response = ApiClient.getService().sendChat(ChatSendRequest(text, currentBackend))
                val updatedList = _messages.value?.toMutableList() ?: mutableListOf()
                updatedList.add(ChatMessage("assistant", response.response, response.backend, response.timestamp))
                _messages.value = updatedList
            } catch (e: Exception) {
                val updatedList = _messages.value?.toMutableList() ?: mutableListOf()
                val errorMsg = if (e.message?.contains("401") == true) {
                    "Error 401: Token expirado. Ve a Config → Cerrar sesión y vuelve a iniciar sesión."
                } else {
                    "Error: ${e.message}"
                }
                updatedList.add(ChatMessage("assistant", errorMsg, currentBackend, System.currentTimeMillis()))
                _messages.value = updatedList
            } finally {
                _sending.value = false
            }
        }
    }

    private fun sendMessageClaude(text: String) {
        _sending.value = true

        // Añadir mensaje del usuario
        val currentList = _messages.value?.toMutableList() ?: mutableListOf()
        currentList.add(ChatMessage("user", text, "claude", System.currentTimeMillis()))
        _messages.value = currentList

        // Señal de inicio de stream (burbuja vacía)
        _streamingText.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            var accumulated = ""
            try {
                val sessionId = getSessionId()
                val bodyJson = JSONObject().apply {
                    put("message", text)
                    if (sessionId != null) put("sessionId", sessionId)
                }.toString()

                val request = Request.Builder()
                    .url(ApiClient.getBaseUrl() + "api/chat/claude-stream")
                    .post(bodyJson.toRequestBody("application/json".toMediaType()))
                    .build()

                ApiClient.getStreamingClient().newCall(request).execute().use { response ->
                    var newSessionId = ""
                    response.body?.source()?.let { source ->
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (!line.startsWith("data: ")) continue
                            try {
                                val event = JSONObject(line.removePrefix("data: "))
                                when (event.optString("type")) {
                                    "text" -> {
                                        accumulated += event.optString("content", "")
                                        val snapshot = accumulated
                                        withContext(Dispatchers.Main) { _streamingText.value = snapshot }
                                    }
                                    "done" -> {
                                        newSessionId = event.optString("sessionId", "")
                                    }
                                    "error" -> {
                                        accumulated = "⚠️ ${event.optString("message", "Error")}"
                                        val snapshot = accumulated
                                        withContext(Dispatchers.Main) { _streamingText.value = snapshot }
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                    }
                    if (newSessionId.isNotEmpty()) saveSessionId(newSessionId)
                }
            } catch (e: Exception) {
                accumulated = "Error: ${e.message}"
                withContext(Dispatchers.Main) { _streamingText.value = accumulated }
            } finally {
                val finalText = accumulated.ifEmpty { "Sin respuesta" }
                withContext(Dispatchers.Main) {
                    _streamingText.value = null  // fin del stream
                    val finalList = _messages.value?.toMutableList() ?: mutableListOf()
                    finalList.add(ChatMessage("assistant", finalText, "claude", System.currentTimeMillis()))
                    _messages.value = finalList
                    _sending.value = false
                }
            }
        }
    }
}
