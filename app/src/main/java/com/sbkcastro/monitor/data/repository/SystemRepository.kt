package com.sbkcastro.monitor.data.repository

import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.MetricsResponse
import com.sbkcastro.monitor.api.MetricsHistoryResponse
import com.sbkcastro.monitor.api.ProcessesResponse

class SystemRepository {
    private val api = ApiClient.getService()

    suspend fun getMetrics(): MetricsResponse = api.getMetrics()

    suspend fun getMetricsHistory(range: String = "4h"): MetricsHistoryResponse =
        api.getMetricsHistory(range)

    suspend fun getProcesses(): ProcessesResponse = api.getProcesses()
}
