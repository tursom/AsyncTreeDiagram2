package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.treediagram.mod.ReturnData
import cn.tursom.treediagram.utils.Json.gson
import cn.tursom.treediagram.utils.ModException
import cn.tursom.utils.xml.Xml
import cn.tursom.web.HttpContent
import java.io.File
import java.util.logging.Level
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.system.exitProcess

@AbsoluteModPath("close/:message", "close")
@ModPath("close/:message", "close")
class Close : Mod() {
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
        val configFile = File("config.xml")
        configFile.delete()
        suspendCoroutine<Int> { cont ->
            fileThreadPool.execute {
                configFile.outputStream().use {
                    it.write(Xml.toXml(config).toByteArray())
                }
                cont.resume(0)
            }
        }
        exitProcess(0)
    }
}