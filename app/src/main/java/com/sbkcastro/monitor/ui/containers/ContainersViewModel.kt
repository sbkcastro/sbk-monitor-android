package com.sbkcastro.monitor.ui.containers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.ContainerActionRequest
import com.sbkcastro.monitor.api.ContainersResponse
import kotlinx.coroutines.launch

class ContainersViewModel : ViewModel() {
    private val _containers = MutableLiveData<ContainersResponse>()
    val containers: LiveData<ContainersResponse> = _containers

    private val _actionResult = MutableLiveData<String?>()
    val actionResult: LiveData<String?> = _actionResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun loadContainers() {
        _loading.value = true
        viewModelScope.launch {
            try {
                _containers.value = ApiClient.getService().getContainers()
            } catch (e: Exception) {
                _actionResult.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun performAction(type: String, name: String, action: String, lxc: String? = null) {
        viewModelScope.launch {
            try {
                val result = ApiClient.getService().containerAction(type, name, ContainerActionRequest(action, lxc))
                _actionResult.value = result.message
                loadContainers()
            } catch (e: Exception) {
                _actionResult.value = "Error: ${e.message}"
            }
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }
}
