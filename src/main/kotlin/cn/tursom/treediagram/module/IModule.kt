package cn.tursom.treediagram.module

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.utils.Json
import cn.tursom.treediagram.utils.ModException
import cn.tursom.web.HttpContent
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter


interface IModule {
    val modUrlBase get() = if (user == null) "/mod/system/" else "/mod/user/$user/"
    val require: Array<out RequireInfo>? get() = javaClass.getAnnotation(Require::class.java)?.require
    val modPermission: ModPermission? get() = javaClass.getAnnotation(AdminMod::class.java)?.permission
    val prettyJson: Boolean get() = false
    val user: String?
    val version: Int get() = javaClass.getAnnotation(Version::class.java)?.version ?: 0
    val apiVersion: Int get() = javaClass.getAnnotation(ApiVersion::class.java)?.version ?: 0
    val gson get() = Json.gson
    val prettyGson get() = Json.prettyGson

    val modDescription: String
        get() = javaClass.getAnnotation(ModDescription::class.java)?.description ?: "no description"
    val modHelper: String get() = javaClass.getAnnotation(ModHelper::class.java)?.helper ?: "no description"

    val modId
        get() = javaClass.getAnnotation(ModId::class.java)?.id?.let {
            if (it.isEmpty()) arrayOf(javaClass.name!!, javaClass.name.split(".").last())
            else it
        } ?: arrayOf(javaClass.name!!, javaClass.name.split(".").last())

    val routeList: List<String>
        get() {
            val list = ArrayList<String>()
            val clazz = this.javaClass
            list.add(clazz.name)

            val path = clazz.getAnnotation(ModPath::class.java)
            if (path != null) {
                path.paths.forEach {
                    list.add(it)
                }
            } else {
                list.add(clazz.name.split('.').last())
            }
            return list
        }

    val absRouteList: List<String>
        get() {
            val list = ArrayList<String>()
            val clazz = this.javaClass
            val path = clazz.getAnnotation(AbsoluteModPath::class.java) ?: return list
            path.paths.forEach {
                list.add(it)
            }
            return list
        }

    /**
     * 当模组被初始化时被调用
     */
    suspend fun init(user: String?, environment: Environment)

    /**
     * 当模组生命周期结束时被调用
     */
    suspend fun destroy(environment: Environment)

    /**
     * 处理模组调用请求
     * @return 一个用于表示返回数据的对象或者null
     */
    @Throws(Throwable::class)
    suspend fun handle(content: HttpContent, environment: Environment): Any?

    suspend fun bottomHandle(content: HttpContent, environment: Environment) {
        handleJson(content) { handle(content, environment) }
    }

    suspend fun <T> handleJson(content: HttpContent, action: suspend () -> T) {
        val ret = try {
            ReturnData(true, action())
        } catch (e: ModException) {
            ReturnData(false, e.message)
        } catch (e: Exception) {
            val writer = StringWriter()
            e.printStackTrace(PrintWriter(writer))
            ReturnData(false, writer)
        }
        content.handleJson(ret)
    }

    suspend fun HttpContent.handleText(text: String) {
        setResponseHeader("content-type", "text/plain; charset=UTF-8")
        finish(text.toByteArray())
    }

    suspend fun HttpContent.handleHtml(text: String) {
        setResponseHeader("content-type", "text/html; charset=UTF-8")
        finish(text.toByteArray())
    }

    suspend fun HttpContent.handleJson(json: Any?) {
        setResponseHeader("content-type", "application/json; charset=UTF-8")
        finish((if (prettyJson) prettyGson.toJson(json)!! else gson.toJson(json)!!).toByteArray())
    }

    val uploadRootPath get() = "upload/"
    val uploadPath: String
        get() {
            val path = if (user == null) {
                "upload/system/"
            } else {
                "upload/user/$user/"
            }
            if (!File(path).exists()) File(path).mkdirs()
            return path
        }

    fun getUploadPath(user: String?): String {
        val path = if (user == null) {
            "upload/system/"
        } else {
            "upload/user/$user/"
        }
        if (!File(path).exists()) File(path).mkdirs()
        return path
    }
}