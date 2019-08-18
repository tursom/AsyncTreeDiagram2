package cn.tursom.treediagram.environment

import java.util.logging.Logger

interface ModEnvironment {
    val logger: Logger
    suspend fun getRouterTree(): String
 }

