package cn.tursom.treediagram.sdk

import cn.tursom.treediagram.TreeDiagramHttpHandler
import cn.tursom.treediagram.utils.Config
import cn.tursom.treediagram.utils.Json.gson
import cn.tursom.utils.AsyncHttpRequest
import cn.tursom.utils.fromJson
import cn.tursom.utils.sha256
import cn.tursom.web.netty.NettyHttpServer

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
class TreeDiagramConnection(val host: String, val port: Int, val https: Boolean = false) {
    constructor(host: String, https: Boolean = false, port: Int = if (https) 443 else 80) : this(host, port, https)

    val url = "${if (https) "https" else "http"}://$host:$port/"
    var token: String = ""

    suspend fun login(user: String, password: String, digest: String.() -> String = { sha256()!! }): Boolean {
        val rPassword = password.digest()
        val data = AsyncHttpRequest.getStr("${url}login/$user", headers = mapOf("password" to rPassword))
        val loginResult = gson.fromJson<LoginResult>(data)
        token = loginResult.result
        return loginResult.state
    }

    suspend fun register(user: String, password: String, digest: String.() -> String = { sha256()!! }): Boolean {
        val rPassword = password.digest()
        val data = AsyncHttpRequest.getStr(
            "${url}register/$user",
            headers = mapOf("password" to rPassword, "token" to token)
        )
        val loginResult = gson.fromJson<LoginResult>(data)
        token = loginResult.result
        return loginResult.state
    }

    suspend fun <T> get(route: String, param: Map<String, String>): RequestResult<T> {
        val data = AsyncHttpRequest.getStr(
            "$url$route",
            param = param,
            headers = mapOf("token" to token)
        )
        return gson.fromJson(data)
    }

    data class LoginResult(val state: Boolean, val result: String)
    data class RequestResult<T>(val state: Boolean, val result: T)
}