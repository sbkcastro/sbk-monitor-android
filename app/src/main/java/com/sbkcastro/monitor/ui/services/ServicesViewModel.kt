package com.sbkcastro.monitor.ui.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class Service(
    val name: String,
    val url: String,
    val icon: String,
    var status: ServiceStatus = ServiceStatus.CHECKING,
    var responseTime: Long = 0
)

enum class ServiceStatus {
    ONLINE,
    OFFLINE,
    CHECKING
}

class ServicesViewModel : ViewModel() {

    private val _services = MutableLiveData<List<Service>>()
    val services: LiveData<List<Service>> = _services

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val servicesList = listOf(
        Service("SBK Castro", "https://sbkcastro.com", "🌐"),
        Service("Eventos", "https://eventos.sbkcastro.com", "📅"),
        Service("DJ App", "https://dj.sbkcastro.com", "🎵"),
        Service("API Monitor", "https://monitor.sbkcastro.com/api/health", "🔌"),
        Service("Uptime Kuma", "http://31.97.55.27:3001", "📊")
    )

    init {
        _services.value = servicesList
    }

    fun checkAllServices() {
        _isLoading.value = true

        viewModelScope.launch {
            val updatedServices = servicesList.map { service ->
                service.copy(status = ServiceStatus.CHECKING)
            }
            _services.postValue(updatedServices)

            val results = servicesList.map { service ->
                withContext(Dispatchers.IO) {
                    checkService(service)
                }
            }

            _services.postValue(results)
            _isLoading.postValue(false)
        }
    }

    private fun checkService(service: Service): Service {
        return try {
            val startTime = System.currentTimeMillis()
            val url = URL(service.url)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            val responseTime = System.currentTimeMillis() - startTime

            connection.disconnect()

            service.copy(
                status = if (responseCode in 200..399) ServiceStatus.ONLINE else ServiceStatus.OFFLINE,
                responseTime = responseTime
            )
        } catch (e: Exception) {
            service.copy(
                status = ServiceStatus.OFFLINE,
                responseTime = 0
            )
        }
    }

    fun addService(name: String, url: String, icon: String) {
        val newService = Service(name, url, icon)
        val currentList = _services.value?.toMutableList() ?: mutableListOf()
        currentList.add(newService)
        _services.value = currentList
    }
}
