package cn.tursom.treediagram.environment

import java.util.logging.Logger

interface RouterEnvironment : LoggerEnvironment {
    suspend fun getRouterTree(): String
}

