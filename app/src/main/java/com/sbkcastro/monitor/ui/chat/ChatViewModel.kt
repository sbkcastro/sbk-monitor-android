package com.sbkcastro.monitor.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.ChatMessage
import com.sbkcastro.monitor.api.ChatSendRequest
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _sending = MutableLiveData<Boolean>()
    val sending: LiveData<Boolean> = _sending

    var currentBackend = "openclaw"

    fun loadHistory() {
        viewModelScope.launch {
            try {
                val history = ApiClient.getService().getChatHistory()
                _messages.value = history.messages
            } catch (_: Exception) { }
        }
    }

    fun sendMessage(text: String) {
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
}
