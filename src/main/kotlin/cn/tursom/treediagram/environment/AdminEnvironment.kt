package cn.tursom.treediagram.environment

import cn.tursom.treediagram.module.IModule
import cn.tursom.treediagram.service.Service
import java.util.logging.FileHandler

interface AdminEnvironment :
    Environment, AdminServiceEnvironment, AdminModEnvironment, AdminRouterEnvironment,
    AdminUserEnvironment, LoggerEnvironment {

    val fileHandler: FileHandler

    override suspend fun registerMod(user: String?, mod: IModule): Boolean = false
    override suspend fun removeMod(user: String?, mod: IModule): Boolean = false

    override suspend fun registerService(user: String?, service: Service): Boolean = false
    override suspend fun removeService(user: String?, service: Service): Boolean = false
}