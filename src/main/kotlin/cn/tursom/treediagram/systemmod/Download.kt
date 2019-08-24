package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.module.AbsoluteModPath
import cn.tursom.treediagram.module.Module
import cn.tursom.treediagram.module.ModPath
import cn.tursom.web.HttpContent
import java.io.File


@AbsoluteModPath("download", "download/:fileName", "download/*")
@ModPath("download", "download/:fileName", "download/*")
class Download : Module() {
    override val modDescription: String = "下载文件"

    override suspend fun handle(content: HttpContent, environment: Environment): Any? {
        val token = environment.token(content)
        val uploadPath = getUploadPath(token.usr!!)
        val file = File("$uploadPath${content["fileName"] ?: return null}")
        if (!file.exists()) return null
        content.setContextType("file")
        content.finishFile(file)
        return file
    }

    override suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        val fileData = handle(content, environment)
        if (fileData == null) {
            content.responseCode = 404
            content.finish()
        }
    }
}