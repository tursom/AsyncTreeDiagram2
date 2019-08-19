package cn.tursom.treediagram.manager.service

import cn.tursom.treediagram.service.ServiceConnection
import kotlinx.coroutines.channels.Channel

class DefaultServiceConnection(
    private val parent: ServiceConnectionDescription,
    private val sendChannel: Channel<Any>,
    private val recvChannel: Channel<Any>
) : ServiceConnection {
    override suspend fun send(message: Any) {
        sendChannel.send(message)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> recv(): T {
        return recvChannel.receive() as T
    }

    override fun close() {
        parent.close()
    }
}