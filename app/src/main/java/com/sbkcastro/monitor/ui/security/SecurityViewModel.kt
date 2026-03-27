package com.sbkcastro.monitor.ui.security

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbkcastro.monitor.api.FirewallResponse
import com.sbkcastro.monitor.api.IDSAlertsResponse
import com.sbkcastro.monitor.api.IDSStatusResponse
import com.sbkcastro.monitor.api.VerifyResponse
import com.sbkcastro.monitor.data.repository.SecurityRepository
import kotlinx.coroutines.launch

class SecurityViewModel : ViewModel() {
    private val repository = SecurityRepository()

    private val _idsStatus = MutableLiveData<IDSStatusResponse>()
    val idsStatus: LiveData<IDSStatusResponse> = _idsStatus

    private val _idsAlerts = MutableLiveData<IDSAlertsResponse>()
    val idsAlerts: LiveData<IDSAlertsResponse> = _idsAlerts

    private val _firewall = MutableLiveData<FirewallResponse>()
    val firewall: LiveData<FirewallResponse> = _firewall

    private val _verify = MutableLiveData<VerifyResponse>()
    val verify: LiveData<VerifyResponse> = _verify

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loadingFirewall = MutableLiveData(false)
    val loadingFirewall: LiveData<Boolean> = _loadingFirewall

    private val _loadingVerify = MutableLiveData(false)
    val loadingVerify: LiveData<Boolean> = _loadingVerify

    fun loadIDS() {
        viewModelScope.launch {
            try {
                _idsStatus.value = repository.getIDSStatus()
                _idsAlerts.value = repository.getIDSAlerts(20)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadFirewall() {
        viewModelScope.launch {
            _loadingFirewall.value = true
            try {
                _firewall.value = repository.getFirewall()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error cargando firewall"
            } finally {
                _loadingFirewall.value = false
            }
        }
    }

    fun loadVerify() {
        viewModelScope.launch {
            _loadingVerify.value = true
            try {
                _verify.value = repository.getVerify()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error verificando servicios"
            } finally {
                _loadingVerify.value = false
            }
        }
    }
}
