package cn.tursom.treediagram.service

import cn.tursom.treediagram.environment.Environment
import cn.tursom.utils.background

abstract class BaseService() : Service {
    override var user: String? = null

    init {
        if (javaClass.getAnnotation(RegisterService::class.java) != null) {
            background {
                //TODO
            }
        }
    }

    override suspend fun receiveMessage(message: Any?, environment: Environment): Any? = null

    override suspend fun getConnection(connection: ServiceConnection, environment: Environment) {
        connection.close()
    }

    override suspend fun initService(user: String?, environment: Environment) {
        this.user = user
//        logger.info("service $id init")
    }

    override suspend fun destroyService(environment: Environment) {
//        logger.info("service $id destroy")
    }
}