package com.sbkcastro.monitor.data.repository

import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.ContainersResponse
import com.sbkcastro.monitor.api.ContainerActionRequest
import com.sbkcastro.monitor.api.ContainerActionResponse
import com.sbkcastro.monitor.api.DockerLogsResponse

class ContainersRepository {
    private val api = ApiClient.getService()

    suspend fun getContainers(): ContainersResponse = api.getContainers()

    suspend fun containerAction(type: String, name: String, action: String): ContainerActionResponse =
        api.containerAction(type, name, ContainerActionRequest(action))

    suspend fun getDockerLogs(lxc: String, container: String, lines: Int = 100): DockerLogsResponse =
        api.getDockerLogs(lxc, container, lines)
}
