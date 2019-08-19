package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.AdminModEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.*
import cn.tursom.treediagram.service.RegisterService
import cn.tursom.treediagram.utils.ModException
import cn.tursom.web.HttpContent

@AbsoluteModPath("loadedMod", "loadedMod/:user")
@ModPath("loadedMod", "loadedMod/:user")
@AdminMod(ModPermission.ModManage)
@RegisterService
@ApiVersion(1)
class LoadedMod : Mod() {
    override val modDescription: String = "返回已经加载的模组"
    @Volatile
    var cacheTime: Long = 0
    @Volatile
    lateinit var cache: LoadedModData

    override suspend fun receiveMessage(message: Any?, environment: Environment): Any? {
        updateCache(environment)
        return cache
    }

    override suspend fun handle(content: HttpContent, environment: Environment): Set<String> {
        updateCache(environment)
        val user = content["user"]
        return if (user == null) cache.systemMod
        else cache.userMod[user] ?: throw ModException("user $user doesn't loaded any mod")
    }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        content.handleJson(handle(content, environment))
    }

    private suspend fun updateCache(environment: Environment) {
        if (cacheTime < environment.modEnvLastChangeTime) {
            environment as AdminModEnvironment
            val pathSet = HashMap<String, Set<String>>()
            environment.modManager.userModMapMap.forEach { s, _ ->
                pathSet[s] = environment.getUserMod(s) ?: return@forEach
            }
            cache = LoadedModData(environment.getSystemMod(), pathSet)
            cacheTime = System.currentTimeMillis()
        }
    }

    data class LoadedModData(val systemMod: Set<String>, val userMod: Map<String, Set<String>>)
}