package cn.tursom.treediagram.service

import cn.tursom.treediagram.environment.Environment

/**
 * 轻量级模组，只对内提供服务
 */
interface Service {
    val serviceId get() = javaClass.getAnnotation(ServiceId::class.java)?.id ?: javaClass.name!!
    val user: String?
    val adminService: Boolean get() = javaClass.getAnnotation(AdminService::class.java) != null

    /**
     * 接受一个对象，处理后立即返回
     */
    suspend fun receiveMessage(message: Any?, environment: Environment): Any? = null

    suspend fun getConnection(connection: ServiceConnection, environment: Environment) {
        connection.close()
    }

    suspend fun initService(user: String?, environment: Environment)
    suspend fun destroyService(environment: Environment)
}

