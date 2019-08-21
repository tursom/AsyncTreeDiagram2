package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.web.HttpContent

@AbsoluteModPath("routerTree", "tree")
@ModPath("routerTree", "tree")
class RouterTree : Mod() {
    override val modDescription: String = "返回路由树"
    private var cacheTime: Long = 0
    private var cache: ByteArray = ByteArray(0)

    override suspend fun handle(content: HttpContent, environment: Environment): ByteArray {
        if (cacheTime != environment.routerLastChangeTime) {
            cache = environment.getRouterTree().toByteArray()
            cacheTime = environment.routerLastChangeTime
        }
        return cache
    }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        if (content.getCacheTag()?.toLongOrNull() == environment.routerLastChangeTime) {
            content.usingCache()
        } else {
            content.setCacheTag(environment.routerLastChangeTime)
            content.finishText(handle(content, environment))
        }
    }
}
