package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.AdminModEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.environment.ModManage
import cn.tursom.treediagram.manager.mod.ClassData
import cn.tursom.treediagram.mod.AdminMod
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.treediagram.mod.ModPermission
import cn.tursom.treediagram.utils.ModException
import cn.tursom.utils.background
import cn.tursom.utils.xml.Constructor
import cn.tursom.utils.xml.Xml
import cn.tursom.web.HttpContent
import kotlinx.coroutines.delay
import org.dom4j.Element
import java.io.File
import java.util.logging.Level

@Suppress("RedundantLambdaArrow")
@ModPath("AutoLoadMod", "AutoLoadMod/:type", "AutoLoadMod/:type/:jar", "AutoLoadMod/:type/:jar/:className")
@AdminMod(ModPermission.ModManage)
class AutoLoadMod : Mod() {
    override val modDescription: String = "在系统启动时自动加载模组"

    override suspend fun init(user: String?, environment: Environment) {
        super.init(user, environment)
        val logger = environment.logger

        if (environment !is ModManage) {
            logger.log(Level.WARNING, "Auto load mod cant get right environment")
        }
        environment as AdminModEnvironment
        val modManager = environment.modManager
        background {
            delay(100)
            @Suppress("SENSELESS_COMPARISON")
            while (modManager == null) delay(20)
            File(uploadRootPath).listFiles { it -> it.isDirectory }?.forEach { path ->
                logger.info("自动加载模组正在加载路径：$path")
                val configXml = File("$path/autoLoad.xml")
                if (!configXml.exists()) return@forEach
                val config = Xml.parse<AutoLoadConfig>(configXml)
                config.jar.forEach forEachConfig@{ (jarName, classes) ->
                    val jarPath = "$path/$jarName"
                    logger.info("自动加载模组正在加载路径jar包：$jarPath")
                    cn.tursom.treediagram.manager.mod.ModLoader.getModLoader(
                        ClassData(jarPath, jarPath, if (classes.isNotEmpty()) classes.toList() else null),
                        path.name,
                        null,
                        true,
                        modManager
                    )
                }
            }
        }
    }

    override suspend fun handle(content: HttpContent, environment: Environment): Any? {
        val type = content["type"] ?: "help"
        if (type == "help") return modHelper

        val token = environment.checkToken(content["token"]!!)
        val user = token.usr!!
        val loadConfigPath = "$uploadRootPath$user/autoLoad.xml"
        val config = Xml.parse<AutoLoadConfig>(File(loadConfigPath))

        if (
            when (type) {
                "addJar" -> {
                    val jarName = content["jar"] ?: throw ModException("无法找到jar文件名")
                    config.jar[jarName] = HashSet()
                    true
                }
                "addClass" -> {
                    val jarName = content["jar"] ?: throw ModException("无法找到jar文件名")
                    val jar = config.jar[jarName] ?: run {
                        val newSet = HashSet<String>()
                        config.jar[jarName] = newSet
                        newSet
                    }
                    jar.add(content["className"] ?: throw ModException("无法找到类名"))
                    true
                }
                "get" -> {
                    false
                }
                else -> {
                    throw ModException("无法找到对应类型")
                }
            }
        ) {
            File("$uploadRootPath$user/autoLoad.xml").delete()
            File("$uploadRootPath$user/autoLoad.xml").outputStream().use {
                it.write(Xml.toXml(config, "config").toByteArray())
            }
        }

        return config
    }

    @Suppress("unused")
    data class AutoLoadConfig(
        @Constructor("setJar") var jar: HashMap<String, HashSet<String>>
    ) {
        fun setJar(element: Element): HashMap<String, HashSet<String>> {
            val map = HashMap<String, HashSet<String>>()
            element.elements("jar").forEach {
                it as Element
                val jarPath = (it.element("name") ?: return@forEach).text ?: return@forEach
                val list = HashSet<String>()
                it.elements("class")?.forEach { clazz ->
                    clazz as Element
                    list.add(clazz.text)
                }
                map[jarPath] = list
            }
            return map
        }
    }
}
