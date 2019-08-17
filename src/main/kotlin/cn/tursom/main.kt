package cn.tursom

import cn.tursom.treediagram.TreeDiagramHttpHandler
import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.web.netty.NettyHttpServer
import cn.tursom.web.router.SuspendRouter

fun main(args: Array<String>) {
    val handler = if (args.size > 1) TreeDiagramHttpHandler(args[1])
    else TreeDiagramHttpHandler()
    val server = NettyHttpServer(handler.config.port, handler)
    server.run()
    println("server started on port ${server.port}")
}