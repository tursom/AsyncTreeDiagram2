package cn.tursom.treediagram.manager.service

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.service.Service
import cn.tursom.utils.background
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout

class ServiceConnectionDescription(
    val service: Service,
    private val environment: Environment,
    private var clientChannel: Channel<Any> = Channel(),
    private var serverChannel: Channel<Any> = Channel()
) : Runnable {
    override fun run() {
        background {
            try {
                withTimeout(60_000) {
                    service.getConnection(serverConnection, environment)
                }
            } finally {
                close()
            }
        }
    }

    val clientConnection get() = DefaultServiceConnection(this, serverChannel, clientChannel)
    private val serverConnection get() = DefaultServiceConnection(this, clientChannel, serverChannel)

    fun newChanel() {
        clientChannel = Channel()
        serverChannel = Channel()

        clientConnection.sendChannel = serverChannel
        clientConnection.recvChannel = clientChannel

        serverConnection.sendChannel = clientChannel
        serverConnection.recvChannel = serverChannel
    }

    fun close() {
        clientChannel.close()
        serverChannel.close()
    }
}