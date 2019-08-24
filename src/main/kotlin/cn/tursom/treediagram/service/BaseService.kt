package cn.tursom.treediagram.service

import cn.tursom.treediagram.environment.Environment

abstract class BaseService() : Service {
    override val serviceUser: String? = null
    override val adminService: Boolean = javaClass.getAnnotation(AdminService::class.java) != null
    override val serviceId: Array<out String> = super.serviceId

    override suspend fun initService(user: String?, environment: Environment) {
        userField.set(this, user)
        environment.logger.info("service $serviceId init")
    }

    override suspend fun destroyService(environment: Environment) {
        environment.logger.info("service $serviceId destroy")
    }

    companion object {
        @JvmStatic
        private val userField = run {
            val field = BaseService::class.java.getDeclaredField("serviceUser")
            field.isAccessible = true
            field
        }
    }
}