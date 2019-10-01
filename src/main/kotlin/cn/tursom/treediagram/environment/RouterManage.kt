package cn.tursom.treediagram.environment

import cn.tursom.treediagram.module.IModule
import cn.tursom.web.router.suspend.impl.SuspendColonStarRouter

interface RouterManage : RouterEnvironment {
    val router: SuspendColonStarRouter<out IModule>

    suspend fun addRouter(mod: IModule, user: String?)
    suspend fun removeRouter(mod: IModule, user: String?)
}