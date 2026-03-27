package com.sbkcastro.monitor.ui.ids

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.IDSAlert
import com.sbkcastro.monitor.api.IDSStatusResponse
import com.sbkcastro.monitor.api.IdsStatsResponse
import kotlinx.coroutines.launch

class IDSDashboardViewModel : ViewModel() {

    private val _idsStatus = MutableLiveData<IDSStatusResponse>()
    val idsStatus: LiveData<IDSStatusResponse> = _idsStatus

    private val _idsStats = MutableLiveData<IdsStatsResponse>()
    val idsStats: LiveData<IdsStatsResponse> = _idsStats

    private val _alerts = MutableLiveData<List<IDSAlert>>()
    val alerts: LiveData<List<IDSAlert>> = _alerts

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        fetchAll()
    }

    fun fetchAll() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val api = ApiClient.getService()
                val status = api.getIDSStatus()
                _idsStatus.value = status

                val stats = api.getIDSStats(24)
                _idsStats.value = stats

                val alertsResponse = api.getIDSAlerts(20)
                _alerts.value = alertsResponse.alerts

            } catch (e: Exception) {
                android.util.Log.e("IDSDashboard", "Error fetching IDS data: ${e.message}", e)
                _error.value = when {
                    e.message?.contains("401") == true -> "Error de autenticación"
                    e.message?.contains("timeout") == true -> "Timeout conectando al servidor"
                    else -> "Error: ${e.message}"
                }
            } finally {
                _loading.value = false
            }
        }
    }
}
