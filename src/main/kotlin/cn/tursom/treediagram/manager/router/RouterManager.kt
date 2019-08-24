package cn.tursom.treediagram.manager.router

import cn.tursom.treediagram.environment.RouterManage
import cn.tursom.treediagram.module.IModule
import cn.tursom.web.router.SuspendRouter
import java.util.logging.Logger

class RouterManager(override val logger: Logger) : RouterManage {
    override val routerLastChangeTime: Long get() = router.lashChangeTime
    override val router: SuspendRouter<IModule> = SuspendRouter()
    override suspend fun getRouterTree(): String = router.suspendToString()

    override suspend fun addRouter(mod: IModule, user: String?) {
        if (user == null) addSystemRouter(mod)
        else addUserRouter(mod, user)
    }

    private suspend fun addUserRouter(mod: IModule, user: String) {
        mod.routeList.forEach {
            addRoute("/mod/user/$user/$it", mod)
            addRoute("/user/$user/$it", mod)
        }
    }

    private suspend fun addSystemRouter(mod: IModule) {
        mod.routeList.forEach {
            addRoute("/mod/system/$it", mod)
        }

        mod.absRouteList.forEach {
            addRoute("/$it", mod)
        }
    }

    private suspend fun addRoute(fullRoute: String, mod: IModule) {
        router.set(fullRoute, mod)
    }

    override suspend fun removeRouter(mod: IModule, user: String?) {
        if (user != null) removeUserMod(mod, user)
        else removeSystemMod(mod)
    }

    private suspend fun removeUserMod(mod: IModule, user: String) {
        mod.routeList.forEach {
            val route = "/mod/user/$user/$it"
            logger.info("try delete route $route")
            if (mod === router.get(route).first) {
                logger.info("delete route $route")
                router.delRoute(route)
            }
        }
    }

    private suspend fun removeSystemMod(mod: IModule) {
        mod.routeList.forEach {
            val route = "/mod/system/$it"
            logger.info("try delete route $route")
            if (mod === router.get(route).first) {
                logger.info("delete route $route")
                router.delRoute(route)
            }
        }

        mod.absRouteList.forEach { route ->
            logger.info("try delete route $route")
            if (mod === router.get(route).first) {
                logger.info("delete route $route")
                router.delRoute(route)
            }
        }
    }
}