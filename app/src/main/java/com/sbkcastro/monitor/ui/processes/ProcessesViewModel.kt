package com.sbkcastro.monitor.ui.processes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbkcastro.monitor.api.ProcessInfo
import com.sbkcastro.monitor.data.repository.SystemRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProcessesViewModel : ViewModel() {
    private val repository = SystemRepository()

    private val _processes = MutableLiveData<List<ProcessInfo>>()
    val processes: LiveData<List<ProcessInfo>> = _processes

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _lastUpdate = MutableLiveData<Long>(0L)
    val lastUpdate: LiveData<Long> = _lastUpdate

    private var autoRefreshJob: kotlinx.coroutines.Job? = null

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = repository.getProcesses()
                _processes.value = resp.processes
                _lastUpdate.value = resp.timestamp
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error cargando procesos"
            } finally {
                _loading.value = false
            }
        }
    }

    fun startAutoRefresh(intervalMs: Long = 5_000L) {
        stopAutoRefresh()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                load()
                delay(intervalMs)
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    override fun onCleared() {
        stopAutoRefresh()
        super.onCleared()
    }
}
