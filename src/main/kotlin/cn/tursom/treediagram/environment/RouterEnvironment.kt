package cn.tursom.treediagram.environment

interface RouterEnvironment : LoggerEnvironment {
    suspend fun getRouterTree(): String
}

