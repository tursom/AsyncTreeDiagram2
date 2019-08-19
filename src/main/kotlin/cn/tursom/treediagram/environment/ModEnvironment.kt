package cn.tursom.treediagram.environment

interface ModEnvironment : LoggerEnvironment {
    val modEnvLastChangeTime: Long
    suspend fun getModTree(user: String?): String
}