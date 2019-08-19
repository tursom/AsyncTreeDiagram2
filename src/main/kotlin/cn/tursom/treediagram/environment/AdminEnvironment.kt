package cn.tursom.treediagram.environment

import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.treediagram.service.Service
import java.util.logging.FileHandler

interface AdminEnvironment :
    Environment, AdminServiceEnvironment, AdminModEnvironment, AdminRouterEnvironment,
    AdminUserEnvironment {

    val fileHandler: FileHandler

    override suspend fun registerMod(user: String?, mod: ModInterface): Boolean = false
    override suspend fun removeMod(user: String?, mod: ModInterface): Boolean = false

    override suspend fun registerService(user: String?, service: Service): Boolean = false
    override suspend fun removeService(user: String?, service: Service): Boolean = false
}

interface AdminModEnvironment : Environment, ModManage
interface AdminServiceEnvironment : Environment, ServiceManage
interface AdminRouterEnvironment : Environment, RouterManage
interface AdminUserEnvironment : Environment, UserManage