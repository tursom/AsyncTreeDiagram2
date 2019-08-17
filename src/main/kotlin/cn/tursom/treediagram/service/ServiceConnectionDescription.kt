package cn.tursom.treediagram.service

import cn.tursom.treediagram.environment.Environment
import cn.tursom.utils.background
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout

class ServiceConnectionDescription(
    val service: Service,
    private val environment: Environment,
    private val clientChannel: Channel<Any> = Channel(),
    private val serverChannel: Channel<Any> = Channel()
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

    val clientConnection =
        ServiceConnection(this, serverChannel, clientChannel)
    private val serverConnection =
        ServiceConnection(this, clientChannel, serverChannel)

    fun close() {
        clientChannel.close()
        serverChannel.close()
    }
}