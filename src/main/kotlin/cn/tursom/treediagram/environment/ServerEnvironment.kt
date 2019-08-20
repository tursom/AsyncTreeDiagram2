package cn.tursom.treediagram.environment

import cn.tursom.treediagram.user.TokenData
import cn.tursom.treediagram.utils.Config
import cn.tursom.treediagram.utils.ModException
import cn.tursom.web.HttpContent

interface ServerEnvironment : LoggerEnvironment {
    val config: Config

    suspend fun checkToken(token: String): TokenData
    suspend fun makeToken(user: String, password: String): String?
    suspend fun token(content: HttpContent) = checkToken(
        getToken(content) ?: throw ModException("no token get")
    )

    suspend fun getToken(content: HttpContent) =
        content.getHeader("token")
            ?: content.getParam("token")
            ?: content.getCookie("token")?.value

    suspend fun makeGuestToken(): String
}