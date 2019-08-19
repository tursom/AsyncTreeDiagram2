package cn.tursom.treediagram.environment

import cn.tursom.treediagram.service.Service

interface ServiceManage : ServiceEnvironment {
    suspend fun registerService(user: String?, service: Service): Boolean
    suspend fun removeService(user: String?, service: Service): Boolean
}