package com.sbkcastro.monitor.api

import retrofit2.http.*

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/health")
    suspend fun health(): HealthResponse

    @GET("api/system/metrics")
    suspend fun getMetrics(): MetricsResponse

    @GET("api/containers")
    suspend fun getContainers(): ContainersResponse

    @POST("api/containers/{type}/{name}/action")
    suspend fun containerAction(
        @Path("type") type: String,
        @Path("name") name: String,
        @Body request: ContainerActionRequest
    ): ContainerActionResponse

    @POST("api/chat/send")
    suspend fun sendChat(@Body request: ChatSendRequest): ChatSendResponse

    @GET("api/chat/history")
    suspend fun getChatHistory(): ChatHistoryResponse

    @GET("api/services/status")
    suspend fun getServicesStatus(): ServicesResponse

    @GET("api/security/alerts")
    suspend fun getSecurityAlerts(): AlertsResponse
}
