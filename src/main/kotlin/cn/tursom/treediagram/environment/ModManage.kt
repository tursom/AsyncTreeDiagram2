package cn.tursom.treediagram.environment

import cn.tursom.aop.aspect.Aspect
import cn.tursom.treediagram.module.IModule
import cn.tursom.utils.datastruct.async.interfaces.AsyncMap
import cn.tursom.utils.datastruct.async.interfaces.AsyncSet

interface ModManage : ModEnvironment {
    val modManager: ModManage
    val systemModMap: AsyncMap<out String, out IModule>
    val userModMapMap: AsyncMap<out String, out AsyncMap<out String, out IModule>>
    val aspectMap: AsyncMap<out String?, out AsyncSet<Aspect>>

    suspend fun getMod(user: String?, modId: String): IModule?
    suspend fun getSystemMod(): Set<String>
    suspend fun getUserMod(user: String?): Set<String>?

    suspend fun registerMod(user: String?, mod: IModule): Boolean
    suspend fun removeMod(user: String?, mod: IModule): Boolean

    suspend fun addAspect(user: String?, aspect: Aspect)
    suspend fun removeAspect(user: String?, aspect: Aspect)
}