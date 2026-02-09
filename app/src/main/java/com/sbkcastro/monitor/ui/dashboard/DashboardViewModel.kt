package com.sbkcastro.monitor.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.MetricsResponse
import com.sbkcastro.monitor.api.ServicesResponse
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val _metrics = MutableLiveData<MetricsResponse>()
    val metrics: LiveData<MetricsResponse> = _metrics

    private val _services = MutableLiveData<ServicesResponse>()
    val services: LiveData<ServicesResponse> = _services

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun loadMetrics() {
        _loading.value = true
        viewModelScope.launch {
            try {
                _metrics.value = ApiClient.getService().getMetrics()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadServices() {
        viewModelScope.launch {
            try {
                _services.value = ApiClient.getService().getServicesStatus()
            } catch (_: Exception) { }
        }
    }
}
