package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.utils.AsyncFile
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.NioAdvanceByteBuffer
import cn.tursom.web.HttpContent
import java.io.File
import java.io.RandomAccessFile


@AbsoluteModPath("download", "download/:fileName", "download/*")
@ModPath("download", "download/:fileName", "download/*")
class Download : Mod() {
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