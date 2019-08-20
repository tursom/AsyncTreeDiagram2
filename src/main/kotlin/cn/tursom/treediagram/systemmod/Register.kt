package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.AdminMod
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.web.HttpContent
import java.util.logging.Level

@AbsoluteModPath("register", "register/:username")
@ModPath("register", "register/:username")
@AdminMod
class Register : Mod() {
    override val modDescription: String = "注册用户"
    override suspend fun handle(content: HttpContent, environment: Environment): String {
        environment as AdminEnvironment
        return environment.registerUser(content)
    }
}