package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.web.HttpContent

@AbsoluteModPath("register", "register/:username")
@ModPath("register", "register/:username")
class Register : Mod() {
    override val modDescription: String = "注册用户"
    override suspend fun handle(content: HttpContent, environment: Environment): String {
        environment as AdminEnvironment
        return environment.registerUser(content)
    }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        content.setResponseHeader("content-type", "application/json; charset=UTF-8")
        content.write(handle(content, environment))
        content.finish()
    }
}