package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.AdminRouterEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.*
import cn.tursom.utils.usingTime
import cn.tursom.web.HttpContent
import cn.tursom.web.netty.NettyAdvanceByteBuffer
import cn.tursom.web.netty.NettyHttpContent
import cn.tursom.web.router.SuspendRouterNode
import cn.tursom.web.utils.EmptyHttpContent
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.lang.Exception
import java.util.logging.Level

@AbsoluteModPath("", "index.html")
@ModPath("", "index.html")
@AdminMod(ModPermission.RouterManage)
class HtmlIndex : Mod() {
    private var cache: String = ""
    private var bytesCache: ByteArray = ByteArray(0)
    private var nettyCache: ByteBuf? = null
    private var cacheTime: Long = 0

    override suspend fun receiveMessage(message: Any?, environment: Environment): Any? {
        environment as AdminEnvironment
        return handle(EmptyHttpContent(), environment)
    }

    private suspend fun toString(
        node: SuspendRouterNode<out ModInterface>,
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
                }${mod.modId[0]}\">$mod</a>" else ""
            }<br />"
        )

        node.forEach {
            toString(it, stringBuilder, if (indentation.isEmpty()) "|" else "$indentation&nbsp;&nbsp;|")
        }
        return
    }

    override suspend fun handle(content: HttpContent, environment: Environment): String {
        environment as AdminRouterEnvironment
        val router = environment.router
        val rootRoute = router.root

        if (cacheTime < environment.router.lashChangeTime) {
            environment.logger.log(Level.INFO, "HtmlIndex flushed cache using ${usingTime {
                val stringBuilder =
                    StringBuilder("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>index</title></head><body>")
                toString(rootRoute, stringBuilder, "")
                stringBuilder.append("</body></html>")
                cache = stringBuilder.toString()
                cacheTime = System.currentTimeMillis()
                bytesCache = cache.toByteArray()
                nettyCache = Unpooled.directBuffer(bytesCache.size)
                nettyCache!!.writeBytes(bytesCache)
            }} ms")
        }
        return cache
    }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        if (content.getCacheTag()?.toLongOrNull() == cacheTime) {
            content.usingCache()
        } else {
            try {
                environment.token(content)
            } catch (e: Exception) {
                content.decodeCookie("token")
                content.addCookie("token", environment.makeGuestToken(), path = "/")
            }
            handle(content, environment)
            content.setCacheTag(cacheTime)
            if (content is NettyHttpContent) {
                content.finishHtml(NettyAdvanceByteBuffer(nettyCache!!.retainedSlice()))
            } else {
                content.finishHtml(bytesCache)
            }
        }
    }
}