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

    @GET("api/chat/presets")
    suspend fun getChatPresets(): PresetsResponse

    @GET("api/services/status")
    suspend fun getServicesStatus(): ServicesResponse

    @GET("api/security/alerts")
    suspend fun getSecurityAlerts(): AlertsResponse

    @GET("api/system/history")
    suspend fun getMetricsHistory(@Query("range") range: String = "4h"): MetricsHistoryResponse

    @GET("api/security/ids")
    suspend fun getIDSStatus(): IDSStatusResponse

    @GET("api/security/ids/alerts")
    suspend fun getIDSAlerts(@Query("limit") limit: Int = 20): IDSAlertsResponse

    @GET("api/containers/docker/{lxc}/{container}/logs")
    suspend fun getDockerLogs(
        @Path("lxc") lxc: String,
        @Path("container") container: String,
        @Query("lines") lines: Int = 100
    ): DockerLogsResponse

    @GET("api/security/ids/stats")
    suspend fun getIDSStats(@Query("hours") hours: Int = 24): IdsStatsResponse

    // ── Claude Remote Tab ──────────────────────────────────────────────────

    @POST("api/claude/job/start")
    suspend fun claudeJobStart(@Body request: ClaudeJobStartRequest): ClaudeJobStartResponse

    @POST("api/claude/job/{id}/message")
    suspend fun claudeJobMessage(
        @Path("id") jobId: String,
        @Body request: ClaudeJobMessageRequest
    ): ClaudeJobMessageResponse

    @POST("api/claude/job/{id}/approve")
    suspend fun claudeJobApprove(@Path("id") jobId: String): ClaudeActionResponse

    @POST("api/claude/job/{id}/deny")
    suspend fun claudeJobDeny(@Path("id") jobId: String): ClaudeActionResponse

    @GET("api/claude/job/pending")
    suspend fun claudeJobPending(): ClaudePendingResponse

    @GET("api/claude/job/sessions")
    suspend fun claudeJobSessions(): ClaudeSessionsResponse

    @DELETE("api/claude/job/{id}")
    suspend fun claudeJobDelete(@Path("id") jobId: String): ClaudeActionResponse

    // NOTE: SSE stream handled manually via OkHttp (not Retrofit)
    // URL: "${ApiClient.getBaseUrl()}api/claude/job/{id}/stream"

    // ── Processes ──────────────────────────────────────────────────────────────
    @GET("api/system/processes")
    suspend fun getProcesses(): ProcessesResponse

    // ── Wazuh SIEM ─────────────────────────────────────────────────────────────
    @GET("api/security/wazuh/alerts")
    suspend fun getWazuhAlerts(@Query("limit") limit: Int = 30): WazuhAlertsResponse

    // ── Firewall ───────────────────────────────────────────────────────────────
    @GET("api/security/firewall")
    suspend fun getFirewall(): FirewallResponse

    // ── Verify ────────────────────────────────────────────────────────────────
    @GET("api/security/verify")
    suspend fun getVerify(): VerifyResponse

    // ── Sites Status ─────────────────────────────────────────────────────────
    @GET("api/sites/status")
    suspend fun getSitesStatus(): SitesStatusResponse

    // ── Notifications ─────────────────────────────────────────────────────────
    @POST("api/notifications/register")
    suspend fun registerFcmToken(@Body request: NotificationRegisterRequest): NotificationRegisterResponse

    // ── Crypto Trade ─────────────────────────────────────────────────────────
    @GET("api/trade/health")
    suspend fun getTradeHealth(): TradeHealthResponse

    @GET("api/trade/prices")
    suspend fun getTradePrices(): List<TradePriceItem>

    @GET("api/trade/status")
    suspend fun getTradeStatus(): TradeStatusResponse

    @GET("api/trade/signals")
    suspend fun getTradeSignals(): TradeSignalsResponse

    @GET("api/trade/portfolio")
    suspend fun getTradePortfolio(): TradePortfolioResponse

    @GET("api/trade/trades")
    suspend fun getTradeTrades(): TradesListResponse

    @GET("api/trade/trades/active")
    suspend fun getTradeTradesActive(): List<TradeItem>

    @GET("api/trade/risk")
    suspend fun getTradeRisk(): TradeRiskResponse

    @GET("api/trade/worldmonitor")
    suspend fun getTradeWorldmonitor(): WorldmonitorAnalysis

    @GET("api/trade/worldmonitor/history")
    suspend fun getTradeWorldmonitorHistory(@Query("limit") limit: Int = 24): List<WorldmonitorAnalysis>
}
