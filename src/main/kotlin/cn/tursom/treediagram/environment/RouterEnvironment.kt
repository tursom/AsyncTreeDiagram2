package cn.tursom.treediagram.environment

interface RouterEnvironment : LoggerEnvironment {
    val routerLastChangeTime: Long
    suspend fun getRouterTree(): String
}

