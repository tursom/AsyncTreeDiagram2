package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.Mod
import cn.tursom.web.HttpContent

@AbsoluteModPath("guest")
class GuestLogin : Mod() {
    override suspend fun handle(content: HttpContent, environment: Environment): Any? {
        val token = environment.makeGuestToken()
        content.deleteCookie("token")
        content.addCookie("token", token, maxAge = 60 * 60 * 24 * 3, path = "/")
        return token
    }
}