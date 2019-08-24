package cn.tursom.treediagram.systemmod

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.module.AbsoluteModPath
import cn.tursom.treediagram.module.Module
import cn.tursom.treediagram.module.ModPath
import cn.tursom.web.HttpContent
import java.io.File

/**
 * 获取上传的文件的列表
 */
@AbsoluteModPath("UploadFileList", "fileList")
@ModPath("UploadFileList", "fileList")
class GetUploadFileList : Module() {
    override val modDescription: String = "获取上传的文件的列表"

    override suspend fun handle(content: HttpContent, environment: Environment): List<String> {
        val token = environment.token(content)
        val uploadPath = "$uploadPath${token.usr}/"
        val uploadDir = File(uploadPath)
        val fileList = ArrayList<String>()
        uploadDir.listFiles()?.forEach {
            fileList.add(it.path.split('/').last())
        }
        return fileList
    }
}
