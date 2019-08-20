package cn.tursom.treediagram.mod

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.utils.Json
import cn.tursom.treediagram.utils.ModException
import cn.tursom.web.HttpContent
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLDecoder
import java.net.URLEncoder

interface ModInterface {
    val require: Array<out RequireInfo>? get() = javaClass.getAnnotation(Require::class.java)?.require
    val modPermission: ModPermission? get() = javaClass.getAnnotation(AdminMod::class.java)?.permission
    val prettyJson: Boolean get() = false
    val user: String?
    val version: Int get() = javaClass.getAnnotation(Version::class.java)?.version ?: 0
    val apiVersion: Int get() = javaClass.getAnnotation(ApiVersion::class.java)?.version ?: 0
    val gson get() = Json.gson
    val prettyGson get() = Json.prettyGson

    val modDescription: String get() = "no description"
    val modHelper: String get() = "no helper"

    val modId get() = javaClass.getAnnotation(ModId::class.java)?.id ?: javaClass.name!!
    val simpModId
        get() = modId.split(".").last()

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
        val ret = try {
            ReturnData(true, handle(content, environment))
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
        write(text)
        finish()
    }

    suspend fun HttpContent.handleHtml(text: String) {
        setResponseHeader("content-type", "text/html; charset=UTF-8")
        write(text)
        finish()
    }

    suspend fun HttpContent.handleJson(json: Any?) {
        setResponseHeader("content-type", "application/json; charset=UTF-8")
        write(if (prettyJson) prettyGson.toJson(json)!! else gson.toJson(json)!!)
        finish()
    }

    val uploadRootPath get() = "upload/"
    fun getUploadPath(user: String) = "$uploadRootPath$user/"
}