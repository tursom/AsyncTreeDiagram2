package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.*
import cn.tursom.web.HttpContent
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

@AbsoluteModPath("modInfo", "modInfo/:modId", "modInfo/:user/:modId", "help", "help/:modId", "help/:user/:modId")
@ModPath("modInfo", "modInfo/:modId", "modInfo/:user/:modId", "help", "help/:modId", "help/:user/:modId")
@AdminMod
class Help : Mod() {
    override val modDescription: String = "查看模组信息"

    private val modMap = ConcurrentHashMap<Pair<String?, ModInterface>, SoftReference<String>>()

    override suspend fun handle(content: HttpContent, environment: Environment): String? {
        environment as AdminEnvironment
        val modManager = environment.modManager

        val user = content["user"] ?: try {
            environment.token(content).usr
        } catch (e: Exception) {
            null
        }
        val modName = content["modId"] ?: "Help"
        val mod = modManager.findMod(modName, user) ?: return null
        val buff = modMap[user to mod]
        val buffStr = buff?.get()
        if (buffStr != null) {
            return buffStr
        }
        val baseRoute = "/mod/${if (user == null) "system" else "user/$user"}/"
        val sb = StringBuilder()
        sb.append("$mod\n")
        val help = mod.modHelper
        if (help.isNotEmpty()) sb.append(help)
        sb.append("\nid:")
        sb.append("\n|- ${mod.modId}")
        if (modManager.findMod(mod.simpModId) === mod) {
            sb.append("\n|- ${mod.simpModId}")
        }
        sb.append("\nrouters:")
        if (user == null) {
            mod.absRouteList.forEach {
                sb.append("\n|- /$it")
            }
        }
        mod.routeList.forEach {
            val route = baseRoute + it
            val routeMod = environment.router.get(route).first
            if (mod === routeMod) {
                sb.append("\n|- $route")
            }
        }
        val str = sb.toString()
        modMap[user to mod] = SoftReference(str)
        return str
    }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        content.handleText(handle(content, environment) ?: "未找到模组")
    }
}