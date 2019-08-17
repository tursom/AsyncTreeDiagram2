package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.web.HttpContent

@AbsoluteModPath("modTree", "modTree/:user", "mod", "mod/system", "mod/:user", "mods", "mods/system", "mods/:user")
@ModPath("modTree", "modTree/:user", "mod", "mod/system", "mod/:user", "mods", "mods/system", "mods/:user")
class ModTree : Mod() {
    override val modDescription: String = "返回模组树"

    override suspend fun handle(content: HttpContent, environment: Environment) =
        if (content.uri == "/mod/system" || content.uri == "/mods/system") {
            environment as AdminEnvironment
            environment.modManager.getModTree("system")
        } else {
            environment as AdminEnvironment
            environment.modManager.getModTree(content["user"])
        }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        content.handleText(handle(content, environment))
    }
}