package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.AdminModEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.module.*
import cn.tursom.web.HttpContent

@AbsoluteModPath("modTree", "modTree/:user", "mod", "mod/system", "mod/:user", "mods", "mods/system", "mods/:user")
@ModPath("modTree", "modTree/:user", "mod", "mod/system", "mod/:user", "mods", "mods/system", "mods/:user")
@AdminMod(ModPermission.ModManage)
class ModTree : Module() {
    override val modDescription: String = "返回模组树"

    override suspend fun handle(content: HttpContent, environment: Environment) =
        if (content.uri == "/mod/system" || content.uri == "/mods/system") {
            environment as AdminModEnvironment
            environment.modManager.getModTree("system")
        } else {
            environment as AdminModEnvironment
            environment.modManager.getModTree(content["user"])
        }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        if (content.getCacheTag()?.toLongOrNull() == environment.modEnvLastChangeTime) {
            content.usingCache()
        } else {
            content.setCacheTag(environment.modEnvLastChangeTime)
            content.handleText(handle(content, environment))
        }
    }
}