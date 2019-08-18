package cn.tursom.treediagram.servicemanager

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.AdminServiceEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.service.ServiceConnection
import cn.tursom.treediagram.service.Service
import cn.tursom.utils.asynclock.AsyncRWLockAbstractMap
import cn.tursom.utils.asynclock.ReadWriteLockHashMap
import kotlinx.coroutines.withTimeout
import java.util.logging.Level

class ServiceManager(val adminEnvironment: AdminEnvironment) : AdminServiceEnvironment {
    override val logger = adminEnvironment.logger
    private val environment = object : Environment by adminEnvironment {}
    private val systemServiceMap: AsyncRWLockAbstractMap<String, Service> = ReadWriteLockHashMap()
    private val serviceMap: AsyncRWLockAbstractMap<String?, AsyncRWLockAbstractMap<String, Service>> =
        ReadWriteLockHashMap()

    override suspend fun registerService(user: String?, service: Service): Boolean {
        logger.log(Level.INFO, "user $user register service ${service.serviceId}")
        if (user == null) {
            registerSystemService(service)
        } else {
            registerUserService(user, service)
        }
        return true
    }

    override suspend fun removeService(user: String?, service: Service): Boolean {
        logger.log(Level.INFO, "user $user remove service ${service.serviceId}")
        val map = if (user != null) serviceMap.get(user) ?: return false
        else systemServiceMap
        map.remove(service.serviceId)
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
        val oldService = systemServiceMap.get(service.serviceId)
        oldService?.destroyService(adminEnvironment)
        service.initService(null, adminEnvironment)
        systemServiceMap.set(service.serviceId, service)
    }

    private suspend fun registerUserService(user: String, service: Service) {
        var map = serviceMap.get(user)
        if (map == null) {
            map = ReadWriteLockHashMap()
            serviceMap.set(user, map)
        }
        val oldService = map.get(service.serviceId)
        oldService?.destroyService(oldService.rightEnv(environment, adminEnvironment))
        service.initService(user, service.rightEnv(environment, adminEnvironment))
        map.set(service.serviceId, service)
    }

    companion object {
        fun Service.rightEnv(environment: Environment, adminEnvironment: AdminEnvironment): Environment {
            return if (adminService) adminEnvironment
            else environment
        }
    }
}