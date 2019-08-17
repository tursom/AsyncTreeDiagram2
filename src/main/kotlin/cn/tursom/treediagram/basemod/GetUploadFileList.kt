package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.AbsoluteModPath
import cn.tursom.treediagram.mod.Mod
import cn.tursom.treediagram.mod.Mod.Companion.uploadRootPath
import cn.tursom.treediagram.mod.ModPath
import cn.tursom.web.HttpContent
import java.io.File

/**
 * 获取上传的文件的列表
 */
@AbsoluteModPath("UploadFileList", "fileList")
@ModPath("UploadFileList", "fileList")
class GetUploadFileList : Mod() {
    override val modDescription: String = "获取上传的文件的列表"

    override suspend fun handle(
        content: HttpContent,
        environment: Environment
    ): List<String> {
        val token = environment.token(content)
        val uploadPath = "$uploadRootPath${token.usr}/"
        val uploadDir = File(uploadPath)
        val fileList = ArrayList<String>()
        uploadDir.listFiles()?.forEach {
            fileList.add(it.path.split('/').last())
        }
        return fileList
    }
}
