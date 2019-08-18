package cn.tursom.treediagram.environment

import cn.tursom.treediagram.utils.Config
import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.treediagram.modloader.ModManager
import cn.tursom.treediagram.service.Service
import cn.tursom.web.HttpContent
import cn.tursom.web.router.SuspendRouter
import java.util.logging.FileHandler

interface AdminEnvironment : Environment {
    val router: SuspendRouter<ModInterface>
    val modManager: ModManager
    val config: Config
    val fileHandler: FileHandler

    suspend fun registerService(user: String?, service: Service): Boolean = false
    suspend fun registerMod(user: String?, mod: ModInterface): Boolean = false
    suspend fun removeService(user: String?, service: Service): Boolean = false
    suspend fun removeMod(user: String?, mod: ModInterface): Boolean = false

    suspend fun registerUser(content: HttpContent): String

    suspend fun getMod(user: String?, modId: String): ModInterface?
}