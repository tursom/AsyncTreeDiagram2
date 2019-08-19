package cn.tursom.treediagram.environment

import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.web.router.SuspendRouter

interface RouterManage : RouterEnvironment {
    val router: SuspendRouter<out ModInterface>

    suspend fun addRouter(mod: ModInterface, user: String?)
    suspend fun removeRouter(mod: ModInterface, user: String?)
}