package cn.tursom.treediagram.manager.service

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.ServiceManage
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.environment.ServiceCaller
import cn.tursom.treediagram.service.ServiceConnection
import cn.tursom.treediagram.service.Service
import cn.tursom.utils.datastruct.async.ReadWriteLockHashMap
import cn.tursom.utils.datastruct.async.collections.AsyncRWLockAbstractMap
import kotlinx.coroutines.withTimeout
import java.util.logging.Level

class ServiceManager(val adminEnvironment: AdminEnvironment) : ServiceManage {
    override val logger = adminEnvironment.logger
    private val environment = object : Environment by adminEnvironment {}
    private val systemServiceMap: AsyncRWLockAbstractMap<String, Service> = ReadWriteLockHashMap()
    private val serviceMap: AsyncRWLockAbstractMap<String?, AsyncRWLockAbstractMap<String, Service>> =
        ReadWriteLockHashMap()

    override suspend fun registerService(user: String?, service: Service): Boolean {
        logger.log(Level.INFO, "user $user register service ${service.javaClass}")
        if (user == null) {
            registerSystemService(service)
        } else {
            registerUserService(user, service)
        }
        return true
    }

    override suspend fun removeService(user: String?, service: Service): Boolean {
        logger.log(Level.INFO, "user $user remove service ${service.javaClass}")
        val map = if (user != null) serviceMap.get(user) ?: return false
        else systemServiceMap
        service.serviceId.forEach {
            map.remove(it)
        }
        return true
    }

    override suspend fun call(user: String?, serviceId: String, message: Any?, timeout: Long): Any? {
        val map = if (user != null) serviceMap.get(user) ?: systemServiceMap else systemServiceMap
        val service = map.get(serviceId)!!
        return withTimeout(timeout) {
            service.receiveMessage(
                message,
                if (service.adminService) adminEnvironment else environment
            )
        }
    }

    override suspend fun getCaller(user: String?, serviceId: String): ServiceCaller? {
        val map = if (user != null) serviceMap.get(user) ?: systemServiceMap else systemServiceMap
        val service = map.get(serviceId) ?: return null
        return DefaultServiceCaller(service, if (service.adminService) adminEnvironment else environment)
    }

    override suspend fun connect(user: String?, serviceId: String): ServiceConnection? {
        val map = serviceMap.get(user) ?: return null
        val service = map.get(serviceId) ?: return null
        val newConnection = ServiceConnectionDescription(
            service,
            if (service.adminService) adminEnvironment else environment
        )
        newConnection.run()
        return newConnection.clientConnection
    }

    private suspend fun registerSystemService(service: Service) {
        service.serviceId.forEach {
            val oldService = systemServiceMap.get(it)
            oldService?.destroyService(adminEnvironment)
            service.initService(null, adminEnvironment)
            systemServiceMap.set(it, service)
        }
    }

    private suspend fun registerUserService(user: String, service: Service) {
        var map = serviceMap.get(user)
        if (map == null) {
            map = ReadWriteLockHashMap()
            serviceMap.set(user, map)
        }
        service.serviceId.forEach {
            val oldService = map.get(it)
            oldService?.destroyService(oldService.rightEnv(environment, adminEnvironment))
            service.initService(user, service.rightEnv(environment, adminEnvironment))
            map.set(it, service)
        }
    }

    override fun toString(): String {
        return systemServiceMap.toString() + serviceMap.toString()
    }

    companion object {
        fun Service.rightEnv(environment: Environment, adminEnvironment: AdminEnvironment): Environment {
            return if (adminService) adminEnvironment
            else environment
        }
    }
}

class DefaultServiceCaller(val service: Service, val environment: Environment) : ServiceCaller {
    override suspend fun call(message: Any?, timeout: Long): Any? {
        return withTimeout(timeout) { service.receiveMessage(message, environment) }
    }
}