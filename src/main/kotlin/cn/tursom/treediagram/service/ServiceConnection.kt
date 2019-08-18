package cn.tursom.treediagram.service

interface ServiceConnection {
    suspend fun send(message: Any)
    suspend fun <T> recv(): T
    fun close()
}