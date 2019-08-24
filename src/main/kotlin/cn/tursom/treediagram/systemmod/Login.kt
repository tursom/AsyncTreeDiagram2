package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.module.AbsoluteModPath
import cn.tursom.treediagram.module.Module
import cn.tursom.treediagram.module.ModPath
import cn.tursom.treediagram.utils.ModException
import cn.tursom.web.HttpContent

@AbsoluteModPath("login", "login/:name")
@ModPath("login", "login/:name")
class Login : Module() {
    override val modDescription: String = "登录"
    override val modHelper: String = "用来登录的模组\n" +
            "参数name用来指定用户名，参数password用来指定密码\n"

    override suspend fun handle(
        content: HttpContent,
        environment: Environment
    ): String {
        val username = content["name"] ?: throw ModException("no user name get")
        val password = content["password"] ?: throw ModException("no password get")
        val token = environment.makeToken(username, password) ?: throw ModException("can't login")
        content.deleteCookie("token")
        content.addCookie("token", token, maxAge = 60 * 10)
        return token
    }
}