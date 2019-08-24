package cn.tursom.treediagram.environment

import cn.tursom.treediagram.module.IModule
import cn.tursom.web.router.SuspendRouter

interface RouterManage : RouterEnvironment {
    val router: SuspendRouter<out IModule>

    suspend fun addRouter(mod: IModule, user: String?)
    suspend fun removeRouter(mod: IModule, user: String?)
}