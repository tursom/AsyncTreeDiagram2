package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.treediagram.utils.ModException
import cn.tursom.web.HttpContent

@AbsoluteModPath("removeMod", "removeMod/:modName", "removeMod/:system/:modName")
@ModPath("removeMod", "removeMod/:modName", "removeMod/:system/:modName")
class ModRemover : Mod() {
    override val modDescription: String = "卸载模组"
    override val modHelper: String = "需要提供token\n" +
            "@param modName 模组名\n" +
            "@param system 是否为系统模组，true为是，其他为否"

    override suspend fun handle(content: HttpContent, environment: Environment): Any? {
        environment as AdminEnvironment
        val modManager = environment.modManager
        val token = environment.token(content)
        val user = token.usr!!
        val modName = content["modName"]!!
        val system = content["system"] == "true"
        modManager.removeMod(
            modManager.findMod(
                modName,
                if (system) {
                    if (token.lev!!.contains("admin")) {
                        null
                    } else {
                        throw ModException("用户无该权限")
                    }
                } else {
                    user
                }
            )
                ?: throw ModException("无法找到模组：$modName")
        )
        return modName
    }
}