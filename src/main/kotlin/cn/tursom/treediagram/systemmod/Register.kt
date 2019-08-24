package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.module.AbsoluteModPath
import cn.tursom.treediagram.module.AdminMod
import cn.tursom.treediagram.module.Module
import cn.tursom.treediagram.module.ModPath
import cn.tursom.web.HttpContent

@AbsoluteModPath("register", "register/:username")
@ModPath("register", "register/:username")
@AdminMod
class Register : Module() {
    override val modDescription: String = "注册用户"
    override suspend fun handle(content: HttpContent, environment: Environment): String {
        environment as AdminEnvironment
        return environment.registerUser(content)
    }
}