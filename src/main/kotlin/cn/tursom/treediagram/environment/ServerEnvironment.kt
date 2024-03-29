package cn.tursom.treediagram.environment

import cn.tursom.treediagram.user.TokenData
import cn.tursom.treediagram.utils.Config
import cn.tursom.treediagram.utils.ModException
import cn.tursom.treediagram.utils.token
import cn.tursom.web.HttpContent

interface ServerEnvironment : LoggerEnvironment {
    val config: Config

    fun tokenStr(content: HttpContent): String?
    suspend fun checkToken(token: String): TokenData
    suspend fun makeToken(user: String, password: String): String?
    suspend fun token(content: HttpContent) = checkToken(
        tokenStr(content) ?: throw ModException("no token get")
    )

    suspend fun makeGuestToken(): String
}