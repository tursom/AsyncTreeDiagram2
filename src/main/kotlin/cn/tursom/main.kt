package cn.tursom

import cn.tursom.treediagram.TreeDiagramHttpHandler
import cn.tursom.web.netty.NettyHttpServer

fun main(args: Array<String>) {
    val handler = if (args.isNotEmpty()) TreeDiagramHttpHandler(args[0])
    else TreeDiagramHttpHandler()
    val server = NettyHttpServer(handler.config.port, handler)
    server.run()
    println(handler.modManager.router.toString())
    println("server started on port ${server.port}")
    println("try url http://127.0.0.1:${server.port}/")
}