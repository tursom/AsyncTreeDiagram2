package cn.tursom.treediagram.environment

import cn.tursom.treediagram.user.TokenData
import cn.tursom.treediagram.utils.Config
import cn.tursom.web.HttpContent
import java.util.logging.Logger

interface ServerEnvironment : LoggerEnvironment {
    val config: Config

    suspend fun checkToken(token: String): TokenData
    suspend fun makeToken(user: String, password: String): String?
    suspend fun token(content: HttpContent) = checkToken(content.getHeader("token") ?: content.getParam("token")!!)
}