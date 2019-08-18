package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.AdminMod
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.web.HttpContent

@AbsoluteModPath("loadedMod", "loadedMod/:user")
@ModPath("loadedMod", "loadedMod/:user")
@AdminMod
class LoadedMod : Mod() {
    override val modDescription: String = "返回已经加载的模组"

    override suspend fun handle(content: HttpContent, environment: Environment): LoadedModData {
        environment as AdminEnvironment
        val modManager = environment.modManager
        return LoadedModData(
            modManager.getSystemMod(),
            modManager.getUserMod(content["user"])
        )
    }

    data class LoadedModData(val systemMod: Set<String>, val userMod: Set<String>?)
}