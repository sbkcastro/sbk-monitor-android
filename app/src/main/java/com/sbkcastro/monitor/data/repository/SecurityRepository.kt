package com.sbkcastro.monitor.data.repository

import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.IDSStatusResponse
import com.sbkcastro.monitor.api.IDSAlertsResponse
import com.sbkcastro.monitor.api.IdsStatsResponse
import com.sbkcastro.monitor.api.FirewallResponse
import com.sbkcastro.monitor.api.VerifyResponse

class SecurityRepository {
    private val api = ApiClient.getService()

    suspend fun getIDSStatus(): IDSStatusResponse = api.getIDSStatus()

    suspend fun getIDSAlerts(limit: Int = 20): IDSAlertsResponse = api.getIDSAlerts(limit)

    suspend fun getIDSStats(hours: Int = 24): IdsStatsResponse = api.getIDSStats(hours)

    suspend fun getFirewall(): FirewallResponse = api.getFirewall()

    suspend fun getVerify(): VerifyResponse = api.getVerify()
}
