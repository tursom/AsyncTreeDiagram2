package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.datastruct.EmailData
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.utils.fromJson
import cn.tursom.web.HttpContent
import java.util.*

/**
 * 用于发送单个邮件的模组
 * 群发邮件请用MultipleEmail和GroupEmail
 *
 * 需要提供的参数为
 *
 * host smtp服务器地址
 * port smtp服务器端口，默认465
 * name 邮箱用户名
 * password 邮箱密码
 * from 发送邮箱
 * to 目标邮箱
 * subject 邮件主题
 * html 邮件主题内容（可为空，为空会使用text）
 * text html为空时的邮件主题内容（html不为空时，此值会被忽略）
 * image 图片（可选）
 * attachment 附件（可选）
 */
@ModPath("mail")
class Email : Mod() {
    override val modDescription: String = "单发邮件"

    override suspend fun receiveMessage(message: Any?, environment: Environment): Any? {
        message as EmailData
        return message.send()
    }

    override suspend fun handle(
        content: HttpContent,
        environment: Environment
    ): String {
        environment.token(content)
        return try {
            //提取邮件信息
            val mailMessage = EmailData(
                content["host"],
                content["port"]?.toIntOrNull() ?: 465,
                content["name"],
                content["password"],
                content["from"],
                content["to"],
                content["subject"],
                content["html"],
                content["text"],
                gson.fromJson<HashMap<String, String>>(content["image"] ?: "[]"),
                gson.fromJson(content["attachment"], Array<String>::class.java)
            )
            //发送邮件
            mailMessage.send()
            //要保持返回值的一致性，所以返回true的字符串值
            "true"
        } catch (cause: Exception) {
            cause.printStackTrace()
            "${cause::class.java}: ${cause.message}"
        }
    }
}

