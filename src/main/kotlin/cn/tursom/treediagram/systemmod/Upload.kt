package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.treediagram.mod.NeedBody
import cn.tursom.treediagram.service.RegisterService
import cn.tursom.treediagram.utils.ModException
import cn.tursom.web.HttpContent
import java.io.File

/**
 * 文件上传模组
 * 需要提供两个参数：
 * filename要上传的文件名称
 * file或者file64
 * file与file64的去别在于file是文本文件的原文件内容，file64是base64编码后的文件内容
 * 返回的是上传到服务器的目录
 */
@AbsoluteModPath("upload/:type/:filename", "upload/:filename", "upload")
@ModPath("upload/:type/:filename", "upload/:filename", "upload")
@NeedBody(10 * 1024 * 1024)
@RegisterService
class Upload : Mod() {
    override val modDescription: String = "上传文件"

    override suspend fun receiveMessage(message: Any?, environment: Environment): Any? {
        message as String?
        return if (message == null) {
            "$uploadRootPath/system"
        } else {
            "$uploadRootPath/$message"
        }
    }

    override suspend fun handle(content: HttpContent, environment: Environment): Any {
        val token = environment.token(content)

        //确保上传用目录可用
        val uploadPath = getUploadPath(token.usr!!)
        if (!File(uploadPath).exists()) {
            File(uploadPath).mkdirs()
        }

        val filename = content["filename"] ?: throw ModException("filename not found")

        val file = File("$uploadPath$filename")

        val stream = when (val uploadType = content.getParam("type")
            ?: content.getHeader("type")
            ?: "append") {
            "create" ->
                if (file.exists()) throw ModException("file exist")
                else file.outputStream()
            "append" -> file.outputStream()
            "delete" -> {
                file.delete()
                return "file \"$filename\" deleted"
            }
            "exist" -> {
                return file.exists()
            }
            else -> throw ModException(
                "unsupported upload type \"$uploadType\", please use one of [\"create\", \"append\"(default), " +
                        "\"delete\", \"exist\"] as an upload type"
            )
        }

        // 写入文件
        fileThreadPool.execute {
            stream.use {
                it.write(content.body, content.bodyOffSet, content.readableBytes)
                it.flush()
            }
        }

        content.setResponseHeader("filename", filename)
        //返回上传的文件名
        return filename
    }
}