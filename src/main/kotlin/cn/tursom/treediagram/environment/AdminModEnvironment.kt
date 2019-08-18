package cn.tursom.treediagram.environment

import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.treediagram.modmanager.ModManager
import cn.tursom.web.router.SuspendRouter

interface AdminModEnvironment {
    val modManager: ModManager
    val router: SuspendRouter<ModInterface>

    suspend fun getMod(user: String?, modId: String): ModInterface?

    suspend fun registerMod(user: String?, mod: ModInterface): Boolean
    suspend fun removeMod(user: String?, mod: ModInterface): Boolean
}