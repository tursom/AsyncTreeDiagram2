package cn.tursom.treediagram.environment

import cn.tursom.treediagram.service.ServiceConnection
import cn.tursom.treediagram.user.TokenData
import cn.tursom.web.HttpContent
import java.util.logging.Logger

interface Environment {
    val logger: Logger

    suspend fun getRouterTree(): String

    suspend fun checkToken(token: String): TokenData
    suspend fun makeToken(user: String, password: String): String?
    suspend fun token(content: HttpContent) = checkToken(content.getHeader("token") ?: content.getParam("token")!!)

    suspend fun call(user: String?, serviceId: String, message: Any?, timeout: Long = 60_000): Any?
    suspend fun connect(user: String?, serviceId: String): ServiceConnection?
}

