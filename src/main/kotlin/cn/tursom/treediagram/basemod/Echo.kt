package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.treediagram.service.RegisterService
import cn.tursom.treediagram.service.ServiceConnection
import cn.tursom.treediagram.service.ServiceId
import cn.tursom.treediagram.utils.ModException
import cn.tursom.web.HttpContent

@AbsoluteModPath("echo", "echo/*", "echo/:message")
@ModPath("echo", "echo/*", "echo/:message")
@ServiceId("Echo")
@RegisterService
class Echo : Mod() {
    override val modDescription: String = "原样返回:message"

    override suspend fun receiveMessage(message: Any?, environment: Environment) = message

    override suspend fun getConnection(connection: ServiceConnection, environment: Environment) {
        while (true) {
            connection.send(connection.recv())
        }
    }

    override suspend fun handle(
        content: HttpContent,
        environment: Environment
    ): String {
        return content["message"] ?: {
            val sb = StringBuilder()
            content.getParams("*")?.forEach {
                sb.append("${it.urlDecode}/")
            } ?: throw ModException("no message get")
            sb.deleteCharAt(sb.length - 1).toString()
        }()
    }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        content.handleText(handle(content, environment))
    }
}