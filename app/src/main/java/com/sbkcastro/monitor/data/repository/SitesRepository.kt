package com.sbkcastro.monitor.data.repository

import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.SitesStatusResponse

class SitesRepository {
    private val api = ApiClient.getService()

    suspend fun getSitesStatus(): SitesStatusResponse = api.getSitesStatus()
}
