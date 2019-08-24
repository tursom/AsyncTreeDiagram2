package cn.tursom.treediagram.environment

import cn.tursom.treediagram.module.IModule
import cn.tursom.utils.datastruct.async.interfaces.AsyncMap

interface ModManage : ModEnvironment {
    val modManager: ModManage
    val systemModMap: AsyncMap<out String, out IModule>
    val userModMapMap: AsyncMap<out String, out AsyncMap<out String, out IModule>>

    suspend fun getMod(user: String?, modId: String): IModule?
    suspend fun getSystemMod(): Set<String>
    suspend fun getUserMod(user: String?): Set<String>?

    suspend fun registerMod(user: String?, mod: IModule): Boolean
    suspend fun removeMod(user: String?, mod: IModule): Boolean
}