package cn.tursom.treediagram.manager.service

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.environment.ServiceCaller
import cn.tursom.treediagram.service.Service
import kotlinx.coroutines.withTimeout

class DefaultServiceCaller(val service: Service, val environment: Environment) :
    ServiceCaller {
    override suspend fun call(message: Any?, timeout: Long): Any? {
        return withTimeout(timeout) { service.receiveMessage(message, environment) }
    }
}