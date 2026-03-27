package com.sbkcastro.monitor.ui.sites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbkcastro.monitor.api.SiteStatus
import com.sbkcastro.monitor.data.repository.SitesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SitesViewModel : ViewModel() {
    private val repository = SitesRepository()

    private val _sites = MutableLiveData<List<SiteStatus>>()
    val sites: LiveData<List<SiteStatus>> = _sites

    private val _summary = MutableLiveData<String>()
    val summary: LiveData<String> = _summary

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
                val resp = repository.getSitesStatus()
                _sites.value = resp.sites
                _summary.value = "${resp.online}/${resp.total} online"
                _lastUpdate.value = System.currentTimeMillis()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error cargando sitios"
            } finally {
                _loading.value = false
            }
        }
    }

    fun startAutoRefresh(intervalMs: Long = 60_000L) {
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
