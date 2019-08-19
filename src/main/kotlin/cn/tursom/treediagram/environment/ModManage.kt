package cn.tursom.treediagram.environment

import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.utils.asynclock.AsyncLockMap

interface ModManage : ModEnvironment {
    val modManager: ModManage
    val systemModMap: AsyncLockMap<out String, out ModInterface>
    val userModMapMap: AsyncLockMap<out String, out AsyncLockMap<out String, out ModInterface>>

    suspend fun getMod(user: String?, modId: String): ModInterface?
    suspend fun getSystemMod(): Set<String>
    suspend fun getUserMod(user: String?): Set<String>?

    suspend fun registerMod(user: String?, mod: ModInterface): Boolean
    suspend fun removeMod(user: String?, mod: ModInterface): Boolean
}