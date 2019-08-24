package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.module.*
import cn.tursom.treediagram.utils.ModException
import cn.tursom.utils.AsyncFile
import cn.tursom.utils.bytebuffer.HeapByteBuffer
import cn.tursom.utils.xml.Xml
import cn.tursom.web.HttpContent
import java.util.logging.Level
import kotlin.system.exitProcess

@AbsoluteModPath("close/:message", "close")
@ModPath("close/:message", "close")
@AdminMod
class Close : Module() {
    override val modDescription: String = "关闭服务器，需要 admin 等级的权限"

    override suspend fun handle(content: HttpContent, environment: Environment): Any? {
        environment as AdminEnvironment
        val token = environment.token(content)
        val logger = environment.logger
        val fileHandler = environment.fileHandler
        val config = environment.config

        if (token.lev?.contains("admin") != true) throw ModException("you are not admin")
        val message = content["message"]
        logger.log(Level.WARNING, "server closed: $message")
        content.write(gson.toJson(ReturnData(true, message)))
        content.finish()
        fileHandler.close()
        val configFile = AsyncFile("config.xml")
        configFile.delete()
        configFile.write(HeapByteBuffer.wrap(Xml.toXml(config)))
        exitProcess(0)
    }
}