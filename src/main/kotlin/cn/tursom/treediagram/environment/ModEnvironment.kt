package cn.tursom.treediagram.environment

interface ModEnvironment {
    suspend fun getRouterTree(): String
 }

