package cn.tursom.treediagram.environment

import cn.tursom.treediagram.user.TokenData
import cn.tursom.web.HttpContent

interface ModEnvironment {
    suspend fun getRouterTree(): String

    suspend fun checkToken(token: String): TokenData
    suspend fun makeToken(user: String, password: String): String?
    suspend fun token(content: HttpContent) = checkToken(content.getHeader("token") ?: content.getParam("token")!!)
}