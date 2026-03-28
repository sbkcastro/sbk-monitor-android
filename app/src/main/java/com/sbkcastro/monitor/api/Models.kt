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
    val uptime: Double,  // Changed from Long to Double (backend sends decimals)
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

data class AuditPreset(val key: String, val label: String, val tag: String)
data class PresetsResponse(val presets: List<AuditPreset>, val timestamp: Long)

data class ChatMessage(val role: String, val content: String, val backend: String, val timestamp: Long)
data class ChatHistoryResponse(val messages: List<ChatMessage>, val total: Int)

data class ServiceStatus(val name: String, val url: String, val status: Int, val up: Boolean)
data class ServicesResponse(val services: List<ServiceStatus>, val timestamp: Long)

data class SecurityAlert(val text: String)
data class AlertsResponse(val alerts: List<String>, val total: Int, val timestamp: Long)

data class HealthResponse(val status: String, val version: String, val timestamp: Long)

// --- Metrics History (server-side, 2min intervals) ---
data class HistoryPoint(val ts: Long, val cpu: Double, val ram: Double, val disk: Double, val load: Double)
data class MetricsHistoryResponse(
    val range: String,
    val interval: String,
    val count: Int,
    val points: List<HistoryPoint>,
    val timestamp: Long
)

// --- IDS (Suricata dual-node) ---
data class IDSNodeInfo(
    val version: String,
    val status: String,
    val rules: Int,
    val events: Int,
    val coverage: String,
    @com.google.gson.annotations.SerializedName("interface") val iface: String
)
data class IDSStatusResponse(
    @com.google.gson.annotations.SerializedName("lxc-vpn") val lxcVpn: IDSNodeInfo,
    @com.google.gson.annotations.SerializedName("lxc-host-gw") val lxcHostGw: IDSNodeInfo,
    val total_coverage: String,
    val total_events: Int,
    val timestamp: Long
)

data class IDSAlert(
    val timestamp: String,
    val source: String,
    val severity: Int,
    val signature: String,
    val category: String,
    val src_ip: String?,
    val dest_ip: String?,
    val count: Int = 1
)
data class IDSAlertsResponse(
    val alerts: List<IDSAlert>,
    val total: Int,
    val timestamp: Long
)

data class DockerLogsResponse(
    val container: String,
    val lxc: String,
    val lines: Int,
    val log: String,
    val timestamp: Long
)

data class MemoryChip(
    val date: String,
    val topics: String,
    val similarity: Double = 0.0
)

// --- IDS Stats (hourly buckets + severity counts) ---
data class HourBucket(
    val hour: Int,
    val count: Int,
    val maxSeverity: Int
)

data class TopIp(
    val ip: String,
    val count: Int,
    val maxSeverity: Int
)

data class IdsStatsResponse(
    val hourlyBuckets: List<HourBucket>,
    val crit: Int,
    val high: Int,
    val med: Int,
    val topIps: List<TopIp>,
    val hours: Int,
    val timestamp: Long
)

// ── Claude Remote Tab ────────────────────────────────────────────────────────

data class ClaudeJobStartRequest(
    val message: String,
    val sessionId: String? = null,
    val model: String? = null
)

data class ClaudeJobStartResponse(
    val jobId: String,
    val sessionId: String?
)

data class ClaudeJobMessageRequest(val message: String)

data class ClaudeJobMessageResponse(val newJobId: String)

data class ClaudePendingApproval(
    val id: String,
    val tool: String,
    val command: String
)

data class ClaudeSession(
    val jobId: String,
    val claudeSessionId: String?,
    val startedAt: Long,
    val lastActivity: Long,
    val messageCount: Int,
    val firstMessage: String,
    val status: String,
    val pendingApproval: ClaudePendingApproval?
)

data class ClaudePendingItem(
    val jobId: String,
    val firstMessage: String,
    val id: String,
    val tool: String,
    val command: String,
    val timestamp: Long
)

data class ClaudeSessionsResponse(val sessions: List<ClaudeSession>)
data class ClaudePendingResponse(val pending: List<ClaudePendingItem>)
data class ClaudeActionResponse(val ok: Boolean? = null, val error: String? = null)

// ── Processes ────────────────────────────────────────────────────────────────
data class ProcessInfo(
    val user: String,
    val pid: Int,
    val cpu: Double,
    val mem: Double,
    val vsz: Int,
    val rss: Int,
    val stat: String,
    val command: String
)
data class ProcessesResponse(val processes: List<ProcessInfo>, val count: Int, val timestamp: Long)

// ── Firewall ─────────────────────────────────────────────────────────────────
data class FailBanSshInfo(val bannedCount: Int, val totalFailed: Int, val detail: String)
data class FailBanInfo(val status: String, val ssh: FailBanSshInfo)
data class FirewallResponse(
    val input: String,
    val forward: String,
    val nat: String,
    val fail2ban: FailBanInfo,
    val timestamp: Long
)

// ── Verify ───────────────────────────────────────────────────────────────────
data class SiteVerify(val name: String, val url: String, val status: Int, val up: Boolean)
data class ServiceVerify(val name: String, val status: String, val active: Boolean)
data class LxcVerify(val name: String, val state: String)
data class DiskVerify(val total: String, val used: String, val available: String, val usePercent: String)
data class VerifyResponse(
    val sites: List<SiteVerify>,
    val services: List<ServiceVerify>,
    val lxc: List<LxcVerify>,
    val disk: DiskVerify,
    val timestamp: Long
)

// ── Sites Status ──────────────────────────────────────────────────────────────
data class SiteStatus(val name: String, val url: String, val status: String, val httpCode: Int, val latency: Long)
data class SitesStatusResponse(val total: Int, val online: Int, val sites: List<SiteStatus>)

// ── Sites Telemetry (uptime + Umami analytics) ────────────────────────────────
data class SiteAnalytics(
    val activeNow: Int,
    val pageviews: Int,
    val visitors: Int,
    val visits: Int,
    val bounceRate: Int?,
    val avgTime: Int?
)
data class SiteTelemetry(
    val name: String, val url: String,
    val status: String, val httpCode: Int, val latency: Long,
    val analytics: SiteAnalytics?
)
data class SitesTelemetryResponse(
    val total: Int, val online: Int,
    val totalActive: Int, val totalPageviews24h: Int,
    val sites: List<SiteTelemetry>
)

// ── Umami History ─────────────────────────────────────────────────────────────
data class UmamiDayPoint(val date: String, val count: Int)
data class UmamiHistoryResponse(
    val site: String,
    val days: Int,
    val pageviews: List<UmamiDayPoint>,
    val sessions: List<UmamiDayPoint>
)

// ── Notifications ─────────────────────────────────────────────────────────────
data class NotificationRegisterRequest(
    val token: String,
    val deviceId: String,
    val platform: String = "android"
)
data class NotificationRegisterResponse(val ok: Boolean, val registered: Int)

// ── Meta-IA ──────────────────────────────────────────────────────────────────
data class MetaChatRequest(
    val message: String,
    val sessionId: String? = null,
    val continueAgent: Boolean = false
)
data class MetaChatResponse(
    val sessionId: String,
    val reply: String,
    val executePrompt: String?,
    val historyLength: Int,
    val agentMode: Boolean = false
)

// ── Wazuh SIEM ───────────────────────────────────────────────────────────────
data class WazuhAlert(
    val timestamp: String,
    val level: Int,
    val rule_id: String,
    val description: String,
    val agent: String,
    val srcip: String?
)
data class WazuhAlertsResponse(
    val alerts: List<WazuhAlert>,
    val total: Int,
    val soar_log: List<String>,
    val timestamp: Long
)

// ── Crypto Trade ─────────────────────────────────────────────────────────────

data class TradeHealthResponse(val status: String, val mode: String?)

data class TradePriceItem(val symbol: String, val price: Double, val timestamp: Long)

data class TradeStatusResponse(
    val bot_name: String?,
    val state: String?,
    val strategy: String?,
    val trading_mode: String?,
    val dry_run: Boolean?
)

data class TradeSignal(
    val pair: String?,
    val profit: Double?,
    val open_date: String?
)
data class TradeSignalsResponse(
    val bot_state: String?,
    val strategy: String?,
    val active_signals: Int?,
    val trades: List<TradeSignal>?
)

data class TradePortfolioResponse(
    val profit: com.google.gson.JsonElement?,
    val balance: com.google.gson.JsonElement?
)

data class TradeItem(
    val pair: String?,
    val profit_ratio: Double?,
    val profit_abs: Double?,
    val open_date: String?,
    val close_date: String?,
    val is_open: Boolean?
)
data class TradesListResponse(
    val trades: List<TradeItem>,
    val trades_count: Int?,
    val total_trades: Int?
)

data class TradeRiskResponse(
    val status: String?,
    val dailyPnl: Double?,
    val drawdown: Double?,
    val balance: Double?,
    val baselineBalance: Double?,
    val limits: TradeRiskLimits?,
    val lastCheck: Long?,
    val alerts: List<String>?
)
data class TradeRiskLimits(
    val dailyLoss: Double?,
    val maxDrawdown: Double?,
    val maxPositionSize: Double?
)

data class WorldmonitorAnalysis(
    val id: Int?,
    val timestamp: Long?,
    val fear_greed: Int?,
    val btc_dominance: Double?,
    val funding_rate: Double?,
    val sentiment: String?,
    val confidence: Double?,
    val recommendation: String?,
    val summary: String?,
    val reasoning: String?
)
