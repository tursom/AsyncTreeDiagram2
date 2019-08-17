package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.web.HttpContent
import cn.tursom.web.router.SuspendRouterNode

@AbsoluteModPath("", "index.html")
@ModPath("", "index.html")
class HtmlIndex : Mod() {
    private var cache: String = ""
    private var cacheTime: Long = 0

    private suspend fun toString(
        node: SuspendRouterNode<ModInterface>,
        stringBuilder: StringBuilder,
        indentation: String
    ) {
        if (node.empty) {
            return
        }
        if (indentation.isNotEmpty()) {
            stringBuilder.append(indentation)
            stringBuilder.append("-&nbsp;")
        }
        val mod = node.value
        stringBuilder.append(
            "<a href=\"${node.fullRoute}\">${node.lastRoute}</a>${
            if (mod != null)
                "&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"/help/${
                if (mod.user != null) "${mod.user}/" else ""
                }${mod.modId}\">$mod</a>" else ""
            }<br />"
        )

        node.forEach {
            toString(it, stringBuilder, if (indentation.isEmpty()) "|" else "$indentation&nbsp;&nbsp;|")
        }
        return
    }

    override suspend fun handle(content: HttpContent, environment: Environment): String {
        environment as AdminEnvironment
        val router = environment.router
        val rootRoute = router.root

        if (cacheTime < environment.router.lashChangeTime) {
            val stringBuilder =
                StringBuilder("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>index</title></head><body>")
            toString(rootRoute, stringBuilder, "")
            stringBuilder.append("</body></html>")
            cache = stringBuilder.toString()
            cacheTime = System.currentTimeMillis()
        }
        return cache
    }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        content.handleHtml(handle(content, environment))
    }
}