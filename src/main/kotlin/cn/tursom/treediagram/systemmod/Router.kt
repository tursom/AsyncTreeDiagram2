package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.AdminModEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.module.*
import cn.tursom.treediagram.utils.ModException
import cn.tursom.web.HttpContent

@AbsoluteModPath("router/:mod", "router/:user/:mod")
@ModPath("router/:mod", "router/:user/:mod")
@AdminMod(ModPermission.ModManage)
class Router : Module() {
    override suspend fun handle(content: HttpContent, environment: Environment): Any? {
        val mod = content["mod"] ?: throw ModException("需要提供参数\"mod\"")
        val user = content["user"]
        environment as AdminModEnvironment
        val modObj = environment.getMod(user, mod) ?: throw ModException("无法找到用户 $user 的模组 $mod")
        val routeList = ArrayList<String>()
        if (user == null) addSystemRoute(modObj, routeList)
        else addUserRoute(modObj, user, routeList)
        return routeList
    }

    private fun addUserRoute(mod: IModule, user: String, routeList: ArrayList<String>) {
        mod.routeList.forEach {
            routeList.add("/mod/user/$user/$it")
            routeList.add("/user/$user/$it")
        }
    }

    private fun addSystemRoute(mod: IModule, routeList: ArrayList<String>) {
        mod.routeList.forEach {
            routeList.add("/mod/system/$it")
        }

        mod.absRouteList.forEach {
            routeList.add("/$it")
        }
    }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        if (content.getCacheTag()?.toLongOrNull() == environment.modEnvLastChangeTime) {
            content.usingCache()
        } else {
            content.setCacheTag(environment.modEnvLastChangeTime)
            super.bottomHandle(content, environment)
        }
    }
}