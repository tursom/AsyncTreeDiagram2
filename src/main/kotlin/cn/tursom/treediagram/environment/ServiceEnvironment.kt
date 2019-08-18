package cn.tursom.treediagram.environment

import cn.tursom.treediagram.servicemanager.ServiceConnection

interface ServiceEnvironment {
    suspend fun call(user: String?, serviceId: String, message: Any?, timeout: Long = 60_000): Any?
    suspend fun connect(user: String?, serviceId: String): ServiceConnection?
}