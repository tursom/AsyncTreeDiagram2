package cn.tursom.treediagram.environment

import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.treediagram.modmanager.ModManager
import cn.tursom.utils.asynclock.AsyncRWLockAbstractMap
import cn.tursom.utils.asynclock.WriteLockHashMap
import cn.tursom.web.router.SuspendRouter

interface AdminModEnvironment {
    val modManager: AdminModEnvironment
    val router: SuspendRouter<ModInterface>
    val modEnvLastChangeTime: Long
    val systemModMap: AsyncRWLockAbstractMap<String, ModInterface>
    val userModMapMap: AsyncRWLockAbstractMap<String, AsyncRWLockAbstractMap<String, ModInterface>>

    suspend fun getMod(user: String?, modId: String): ModInterface?
    suspend fun getSystemMod(): Set<String>
    suspend fun getUserMod(user: String?): Set<String>?
    suspend fun getModTree(user: String?): String

    suspend fun registerMod(user: String?, mod: ModInterface): Boolean
    suspend fun removeMod(user: String?, mod: ModInterface): Boolean
}