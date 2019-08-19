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

    override suspend fun handle(content: HttpContent, environment: Environment) = environment.getRouterTree()
    override suspend fun bottomHandle(content: HttpContent, environment: Environment) =
        content.handleText(handle(content, environment))
}
