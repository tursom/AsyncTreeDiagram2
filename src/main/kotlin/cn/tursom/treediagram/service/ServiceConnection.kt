package cn.tursom.treediagram.service

import kotlinx.coroutines.channels.Channel

class ServiceConnection(
    private val parent: ServiceConnectionDescription,
    private val sendChannel: Channel<Any>,
    private val recvChannel: Channel<Any>
) {
    suspend fun send(message: Any) {
        sendChannel.send(message)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> recv(): T {
        return recvChannel.receive() as T
    }

    fun close() {
        parent.close()
    }
}