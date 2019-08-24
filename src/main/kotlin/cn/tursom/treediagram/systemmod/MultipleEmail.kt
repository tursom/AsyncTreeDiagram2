package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.datastruct.MultipleEmailData
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.module.Module
import cn.tursom.utils.fromJson
import cn.tursom.web.HttpContent


class MultipleEmail : Module() {
    override val modDescription: String = "群发邮件，每个邮件的内容都不同"

    override suspend fun handle(content: HttpContent, environment: Environment): String {
        environment.token(content)
        try {
            val groupEmailData = gson.fromJson<MultipleEmailData>(content["message"]!!)
            groupEmailData.send()
        } catch (cause: Exception) {
            return "${cause::class.java}: ${cause.message}"
        }
        return "true"
    }
}
