package cn.tursom.treediagram.environment

import cn.tursom.treediagram.service.ServiceConnection
import java.util.logging.Logger

interface ServiceEnvironment {
    val logger: Logger
    suspend fun call(user: String?, serviceId: String, message: Any?, timeout: Long = 60_000): Any?
    suspend fun connect(user: String?, serviceId: String): ServiceConnection?
}