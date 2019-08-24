package cn.tursom.treediagram.environment

interface ServiceCaller {
    suspend fun call(message: Any?, timeout: Long = 60_000): Any?
    suspend operator fun invoke(message: Any?, timeout: Long = 60_000) = call(message, timeout)
}