package cn.tursom.treediagram.environment

import java.util.logging.Logger

interface ModEnvironment  {
    val logger: Logger
    val modEnvLastChangeTime: Long
    suspend fun getModTree(user: String?): String
}

