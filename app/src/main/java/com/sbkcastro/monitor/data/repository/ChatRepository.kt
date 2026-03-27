package com.sbkcastro.monitor.data.repository

import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.ClaudeJobStartRequest
import com.sbkcastro.monitor.api.ClaudeJobStartResponse
import com.sbkcastro.monitor.api.ClaudeJobMessageRequest
import com.sbkcastro.monitor.api.ClaudeJobMessageResponse
import com.sbkcastro.monitor.api.ClaudeSessionsResponse
import com.sbkcastro.monitor.api.ClaudeActionResponse
import com.sbkcastro.monitor.api.NotificationRegisterRequest
import com.sbkcastro.monitor.api.NotificationRegisterResponse

class ChatRepository {
    private val api = ApiClient.getService()

    suspend fun startClaudeJob(message: String, sessionId: String? = null): ClaudeJobStartResponse =
        api.claudeJobStart(ClaudeJobStartRequest(message, sessionId))

    suspend fun sendClaudeMessage(jobId: String, message: String): ClaudeJobMessageResponse =
        api.claudeJobMessage(jobId, ClaudeJobMessageRequest(message))

    suspend fun approveJob(jobId: String): ClaudeActionResponse = api.claudeJobApprove(jobId)

    suspend fun denyJob(jobId: String): ClaudeActionResponse = api.claudeJobDeny(jobId)

    suspend fun getSessions(): ClaudeSessionsResponse = api.claudeJobSessions()

    suspend fun deleteJob(jobId: String): ClaudeActionResponse = api.claudeJobDelete(jobId)

    suspend fun registerFcmToken(token: String, deviceId: String): NotificationRegisterResponse =
        api.registerFcmToken(NotificationRegisterRequest(token, deviceId))
}
