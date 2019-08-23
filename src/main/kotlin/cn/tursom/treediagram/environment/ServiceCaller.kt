package cn.tursom.treediagram.environment

interface ServiceCaller {
    suspend fun call(message: Any?, timeout: Long = 60_000): Any?
}