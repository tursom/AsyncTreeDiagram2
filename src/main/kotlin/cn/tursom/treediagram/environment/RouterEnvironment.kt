package cn.tursom.treediagram.environment

import java.util.logging.Logger

interface RouterEnvironment {
    val logger: Logger
    suspend fun getRouterTree(): String
}

