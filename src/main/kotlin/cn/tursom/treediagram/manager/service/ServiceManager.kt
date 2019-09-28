package cn.tursom.treediagram.manager.service

import cn.tursom.aop.ProxyHandler
import cn.tursom.aop.aspect.Aspect
import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.environment.ServiceCaller
import cn.tursom.treediagram.environment.ServiceManage
import cn.tursom.treediagram.service.Service
import cn.tursom.treediagram.service.ServiceConnection
import cn.tursom.utils.cache.DefaultAsyncPotableCacheMap
import cn.tursom.utils.cache.interfaces.AsyncPotableCacheMap
import cn.tursom.utils.datastruct.async.ReadWriteLockHashMap
import cn.tursom.utils.datastruct.async.collections.AsyncMapSet
import cn.tursom.utils.datastruct.async.collections.AsyncRWLockAbstractMap
import cn.tursom.utils.datastruct.async.interfaces.AsyncPotableMap
import cn.tursom.utils.datastruct.async.interfaces.AsyncPotableSet
import cn.tursom.utils.datastruct.async.interfaces.AsyncSet
import kotlinx.coroutines.withTimeout
import java.util.logging.Level

class ServiceManager(val adminEnvironment: AdminEnvironment) : ServiceManage {
    override val logger = adminEnvironment.logger
    private val environment = object : Environment by adminEnvironment {}
    private val systemServiceMap: AsyncRWLockAbstractMap<String, Service> = ReadWriteLockHashMap()
    private val serviceMap: AsyncRWLockAbstractMap<String?, AsyncRWLockAbstractMap<String, Service>> =
        ReadWriteLockHashMap()
    private val naturalSystemServiceMap: AsyncRWLockAbstractMap<String, Service> = ReadWriteLockHashMap()
    private val naturalServiceMap: AsyncRWLockAbstractMap<String?, AsyncRWLockAbstractMap<String, Service>> =
        ReadWriteLockHashMap()
    override val aspectMap: AsyncPotableCacheMap<String?, AsyncPotableSet<Aspect>> = DefaultAsyncPotableCacheMap(5)

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
        val naturalMap = if (user != null) naturalServiceMap.get(user) ?: return false
        else naturalSystemServiceMap
        service.serviceId.forEach {
            map.remove(it)
            naturalMap.remove(it)
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
        val proxiedService = aspectMap.get(null)?.let {
            proxyEnhance(it, service)
        } ?: service
        service.serviceId.forEach {
            val oldService = systemServiceMap.get(it)
            oldService?.destroyService(adminEnvironment)
            service.initService(null, adminEnvironment)

            systemServiceMap.set(it, proxiedService)
            naturalSystemServiceMap.set(it, service)
        }
    }

    private suspend fun registerUserService(user: String, service: Service) {
        val proxiedService = aspectMap.get(null)?.let {
            proxyEnhance(it, service)
        } ?: service

        var map = serviceMap.get(user)
        if (map == null) {
            map = ReadWriteLockHashMap()
            serviceMap.set(user, map)
        }
        var naturalMap = naturalServiceMap.get(user)
        if (naturalMap == null) {
            naturalMap = ReadWriteLockHashMap()
            naturalServiceMap.set(user, map)
        }
        service.serviceId.forEach {
            val oldService = map.get(it)
            oldService?.destroyService(oldService.rightEnv(environment, adminEnvironment))
            service.initService(user, service.rightEnv(environment, adminEnvironment))
            map.set(it, proxiedService)
            naturalMap.set(it, service)
        }
    }

    private suspend fun proxyEnhance(aspects: AsyncSet<Aspect>, service: Service): Service {
        val clazz = service.javaClass
        var proxiedService = service
        aspects.forEach {
            if (it.pointcut.matchClass(clazz)) {
                proxiedService = ProxyHandler.proxyEnhance(proxiedService, it) as Service
            }
            true
        }
        return proxiedService
    }

    private suspend fun getUserMap(user: String?): AsyncPotableMap<String, Service>? =
        if (user != null) serviceMap.get(user) else systemServiceMap

    private suspend fun getNaturalUserMap(user: String?): AsyncPotableMap<String, Service>? =
        if (user != null) naturalServiceMap.get(user) else naturalSystemServiceMap

    override suspend fun addAspect(user: String?, aspect: Aspect) {
        val aspects = aspectMap.get(user) { AsyncMapSet() }
        if (aspects.putIfAbsent(aspect)) {
            val map = getUserMap(user) ?: return
            val naturalMap = getNaturalUserMap(user) ?: return
            naturalMap.forEach { (id, service) ->
                if (aspect.pointcut.matchClass(service.javaClass)) {
                    map.set(id, ProxyHandler.proxyEnhance(map.get(id) ?: return@forEach true, aspect) as Service)
                }
                true
            }
        }
    }

    override suspend fun removeAspect(user: String?, aspect: Aspect) {
        val aspects = aspectMap.get(user) ?: return
        if (!aspects.contains(aspect)) return

        aspects.remove(aspect)
        val map = getUserMap(user) ?: return
        val naturalMap = getNaturalUserMap(user) ?: return
        naturalMap.forEach { (id, service) ->
            map.set(id, proxyEnhance(aspects, service))
            true
        }
    }

    override fun toString(): String {
        return naturalSystemServiceMap.toString() + naturalServiceMap.toString()
    }

    companion object {
        fun Service.rightEnv(environment: Environment, adminEnvironment: AdminEnvironment): Environment {
            return if (adminService) adminEnvironment
            else environment
        }
    }
}

