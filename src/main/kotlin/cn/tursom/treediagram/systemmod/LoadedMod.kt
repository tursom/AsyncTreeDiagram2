package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.*
import cn.tursom.treediagram.service.RegisterService
import cn.tursom.web.HttpContent

@AbsoluteModPath("loadedMod", "loadedMod/:user")
@ModPath("loadedMod", "loadedMod/:user")
@AdminMod
@RegisterService
class LoadedMod : Mod() {
    override val modDescription: String = "返回已经加载的模组"
    @Volatile
    var cacheTime: Long = 0
    lateinit var cache: LoadedModData

    override suspend fun receiveMessage(message: Any?, environment: Environment): Any? {
        environment as AdminEnvironment
        if (cacheTime < environment.modManager.modEnvLastChangeTime) {
            val modManager = environment.modManager
            val pathSet = ArrayList<Pair<String, Set<String>>>()
            modManager.userModMapMap.forEach { s, asyncRWLockAbstractMap ->
                val userSet = HashSet<String>()
                asyncRWLockAbstractMap.forEach { _: String, u: ModInterface ->
                    u.routeList.forEach {
                        userSet.add(it)
                    }
                }
                pathSet.add(s to userSet)
            }
            cache = LoadedModData(modManager.getSystemMod(), pathSet)
        }
        return cache
    }

    override suspend fun handle(content: HttpContent, environment: Environment): Set<String> {
        environment as AdminEnvironment
        val modManager = environment.modManager
        val user = content["user"]
        return if (user == null) modManager.getSystemMod()
        else modManager.getUserMod(user)!!

    }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        content.handleJson(handle(content, environment))
    }

    data class LoadedModData(val systemMod: Set<String>, val userMod: List<Pair<String, Set<String>>>?)
}