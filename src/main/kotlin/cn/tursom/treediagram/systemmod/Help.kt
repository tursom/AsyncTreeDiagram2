package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.module.*
import cn.tursom.web.HttpContent
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

@AbsoluteModPath("modInfo", "modInfo/:modId", "modInfo/:user/:modId", "help", "help/:modId", "help/:user/:modId")
@ModPath("modInfo", "modInfo/:modId", "modInfo/:user/:modId", "help", "help/:modId", "help/:user/:modId")
@AdminMod
class Help : Module() {
    override val modDescription: String = "查看模组信息"

    private val modCacheMap = ConcurrentHashMap<Pair<String?, Array<out String>>, ModHelperCache>()

    override suspend fun handle(content: HttpContent, environment: Environment): ByteArray? {
        environment as AdminEnvironment
        val modManager = environment.modManager

        val user = content["user"] ?: try {
            environment.token(content).usr
        } catch (e: Exception) {
            null
        }
        val modName = content["modId"] ?: "Help"
        val mod = modManager.getMod(user, modName) ?: return null
        val cache = modCacheMap[user to mod.modId]
        val cacheStr = cache?.data?.get()
        if (cache != null && cache.cacheTime > environment.modEnvLastChangeTime && cacheStr != null) {
            return cacheStr
        }
        val baseRoute = "/mod/${if (user == null) "system" else "user/$user"}/"
        val sb = StringBuilder()
        sb.append("$mod\n")
        val help = mod.modHelper
        if (help.isNotEmpty()) sb.append(help)
        sb.append("\nid:")
        mod.modId.forEach {
            sb.append("\n|- $it")
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
        val str = sb.toString().toByteArray()
        modCacheMap[user to mod.modId] = ModHelperCache(str)
        return str
    }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        if (content.getCacheTag()?.toLongOrNull() == environment.modEnvLastChangeTime) {
            content.usingCache()
        } else {
            content.setCacheTag(environment.modEnvLastChangeTime)
            content.finishText(handle(content, environment) ?: "未找到模组".toByteArray())
        }
    }

    data class ModHelperCache(val data: SoftReference<ByteArray>, val cacheTime: Long = System.currentTimeMillis()) {
        constructor(data: String, cacheTime: Long = System.currentTimeMillis()) : this(data.toByteArray(), cacheTime)
        constructor(data: ByteArray, cacheTime: Long = System.currentTimeMillis()) :
                this(SoftReference(data), cacheTime)
    }
}