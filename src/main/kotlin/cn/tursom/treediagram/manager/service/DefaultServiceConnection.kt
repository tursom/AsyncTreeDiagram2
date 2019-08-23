package cn.tursom.treediagram.manager.service

import cn.tursom.treediagram.service.ServiceConnection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException

class DefaultServiceConnection(
    private val parent: ServiceConnectionDescription,
    var sendChannel: Channel<Any>,
    var recvChannel: Channel<Any>
) : ServiceConnection {
    override suspend fun send(message: Any) {
        try {
            sendChannel.send(message)
        } catch (e: ClosedSendChannelException) {
            parent.newChanel()
            sendChannel.send(message)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> recv(): T {
        return recvChannel.receive() as T
    }

    override fun close() {
        parent.close()
    }
}