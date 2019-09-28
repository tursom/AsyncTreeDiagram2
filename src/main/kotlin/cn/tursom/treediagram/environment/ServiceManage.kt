package cn.tursom.treediagram.environment

import cn.tursom.aop.aspect.Aspect
import cn.tursom.treediagram.service.Service
import cn.tursom.utils.datastruct.async.interfaces.AsyncMap
import cn.tursom.utils.datastruct.async.interfaces.AsyncSet

interface ServiceManage : ServiceEnvironment {
    suspend fun registerService(user: String?, service: Service): Boolean
    suspend fun removeService(user: String?, service: Service): Boolean

    val aspectMap: AsyncMap<out String?, out AsyncSet<Aspect>>
    suspend fun addAspect(user: String?, aspect: Aspect)
    suspend fun removeAspect(user: String?, aspect: Aspect)
}