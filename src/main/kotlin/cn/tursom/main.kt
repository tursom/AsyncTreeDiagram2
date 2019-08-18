package cn.tursom

import cn.tursom.treediagram.TreeDiagramHttpHandler
import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.treediagram.modloader.ClassData
import cn.tursom.treediagram.modloader.ModLoader
import cn.tursom.treediagram.service.Service
import cn.tursom.web.netty.NettyHttpServer
import cn.tursom.web.router.SuspendRouter

suspend fun main(args: Array<String>) {
    val handler = if (args.size > 1) TreeDiagramHttpHandler(args[1])
    else TreeDiagramHttpHandler()
    val server = NettyHttpServer(handler.config.port, handler)
    server.run()
    println(handler.router.toString())
    println("server started on port ${server.port}")
    println("try url http://127.0.0.1:${server.port}/")
    val testMod = ModLoader.getModLoader(
        ClassData(null, "TestMod-1.0-SNAPSHOT.jar", null), "tursom", modManager = handler.modManager
    )
    val requireMod = ModLoader.getModLoader(
        ClassData(null, "RequireTest-1.0-SNAPSHOT.jar", null), "tursom", modManager = handler.modManager
    )
    testMod.load()
    requireMod.load()
    val service = handler.modManager.getMod("tursom", "RequireTest") as Service
    val serviceId = service.serviceId
    try {
        println(handler.environment.call(service.user, serviceId, "hello"))
    } catch (e: NoClassDefFoundError) {
        e.cause?.printStackTrace()
    }
}