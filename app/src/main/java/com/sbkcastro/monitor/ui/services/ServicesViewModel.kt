package com.sbkcastro.monitor.ui.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.SitesTelemetryResponse
import kotlinx.coroutines.launch

class ServicesViewModel : ViewModel() {

    private val _telemetry = MutableLiveData<SitesTelemetryResponse?>()
    val telemetry: LiveData<SitesTelemetryResponse?> = _telemetry

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun load() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                _telemetry.value = ApiClient.getService().getSitesTelemetry()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
