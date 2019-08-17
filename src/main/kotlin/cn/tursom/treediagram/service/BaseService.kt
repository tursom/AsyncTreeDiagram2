package cn.tursom.treediagram.service

import cn.tursom.treediagram.environment.Environment
import cn.tursom.utils.background

abstract class BaseService() : Service {
    override var user: String? = null

    override suspend fun receiveMessage(message: Any?, environment: Environment): Any? = null

    override suspend fun getConnection(connection: ServiceConnection, environment: Environment) {
        connection.close()
    }

    override suspend fun initService(user: String?, environment: Environment) {
        this.user = user
        environment.logger.info("service $serviceId init")
    }

    override suspend fun destroyService(environment: Environment) {
        environment.logger.info("service $serviceId destroy")
    }
}