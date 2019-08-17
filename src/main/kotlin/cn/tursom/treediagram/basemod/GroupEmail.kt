package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.datastruct.GroupEmailData
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.Mod
import cn.tursom.utils.bytebuffer.fromJson
import cn.tursom.web.HttpContent
import com.google.gson.Gson
import java.util.*

/**
 * 用于群发邮件的模组
 * 为每个人发送内容相同的邮件
 */
class GroupEmail : Mod() {
    override val modDescription: String = "群发邮件，为每个人发送内容相同的邮件"

    override suspend fun handle(content: HttpContent, environment: Environment): Any? {
        environment.token(content)
        try {
            val groupEmailData = GroupEmailData(
                content["host"],
                content["port"]?.toInt(),
                content["name"],
                content["password"],
                content["from"],
                gson.fromJson(content["to"], Array<String>::class.java),
                content["subject"],
                content["html"],
                content["text"],
                gson.fromJson<HashMap<String, String>>(content["image"] ?: "[]"),
                gson.fromJson(content["attachment"], Array<String>::class.java)
            )
            groupEmailData.send()
        } catch (cause: Exception) {
            return "${cause::class.java}: ${cause.message}"
        }
        return "true"
    }

    companion object {
        private val gson = Gson()
    }
}
