package com.sbkcastro.monitor.api

data class LoginRequest(val password: String)
data class LoginResponse(val token: String, val expiresIn: Long)

data class CpuInfo(val usage: Double, val cores: Int)
data class MemoryInfo(val total: Long, val used: Long, val free: Long, val available: Long, val usagePercent: Double)
data class SwapInfo(val total: Long, val used: Long, val free: Long, val usagePercent: Double)
data class DiskInfo(val total: Long, val used: Long, val available: Long, val usagePercent: Double, val mount: String)
data class MetricsResponse(
    val cpu: CpuInfo,
    val memory: MemoryInfo,
    val swap: SwapInfo,
    val disk: DiskInfo,
    val uptime: Long,
    val loadAvg: List<Double>,
    val hostname: String,
    val timestamp: Long
)

data class DockerContainer(val name: String, val status: String, val ports: String, val running: Boolean)
data class LxcContainer(
    val name: String,
    val state: String,
    val ip: String?,
    val memory: String,
    val dockerContainers: List<DockerContainer>
)
data class ContainersResponse(val containers: List<LxcContainer>, val timestamp: Long)

data class ContainerActionRequest(val action: String, val lxc: String? = null)
data class ContainerActionResponse(val success: Boolean, val message: String)

data class ChatSendRequest(val message: String, val backend: String)
data class ChatSendResponse(val response: String, val backend: String, val timestamp: Long)

data class ChatMessage(val role: String, val content: String, val backend: String, val timestamp: Long)
data class ChatHistoryResponse(val messages: List<ChatMessage>, val total: Int)

data class ServiceStatus(val name: String, val url: String, val status: Int, val up: Boolean)
data class ServicesResponse(val services: List<ServiceStatus>, val timestamp: Long)

data class SecurityAlert(val text: String)
data class AlertsResponse(val alerts: List<String>, val total: Int, val timestamp: Long)

data class HealthResponse(val status: String, val version: String, val timestamp: Long)
