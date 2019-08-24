package cn.tursom.treediagram.module

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.service.AdminService
import cn.tursom.treediagram.service.BaseService
import java.io.File
import java.lang.reflect.Field
import java.util.logging.Level

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class Module : IModule, BaseService() {
    override val user: String? = null
    override val require = javaClass.getAnnotation(Require::class.java)?.require
    override val version: Int = javaClass.getAnnotation(Version::class.java)?.version ?: 0
    override val apiVersion: Int = javaClass.getAnnotation(ApiVersion::class.java)?.version ?: 0
    override val adminService: Boolean = javaClass.getAnnotation(AdminService::class.java) != null
    override val routeList: List<String> = super.routeList
    override val absRouteList: List<String> = super.absRouteList
    override val modId: Array<out String> = super.modId
    override val modPermission: ModPermission? = super.modPermission
    override val modDescription: String = super.modDescription
    override val modHelper: String = super.modHelper
    override val modUrlBase: String by lazy { super.modUrlBase }

    /**
     * 模组私有目录
     * 在调用的时候会自动创建目录，不必担心目录不存在的问题
     * 如果有模组想储存文件请尽量使用这个目录
     */
    val modPath by lazy {
        val path = "mods/${if (serviceUser != null) "user/$serviceUser/" else "syste/"}${this::class.java.name}/"
        val dir = File(path)
        if (!dir.exists()) dir.mkdirs()
        path
    }

    /** mod 生命周期 */

    /**
     * 当模组被初始化时被调用
     */
    override suspend fun init(user: String?, environment: Environment) {
        userField.set(this, user)
        environment.logger.log(Level.INFO, "mod $javaClass init by $user")
    }

    /**
     * 当模组生命周期结束时被调用
     */
    override suspend fun destroy(environment: Environment) {
        environment.logger.log(Level.INFO, "mod $javaClass destroy")
    }

    /** utils */

    override fun toString(): String {
        return "${modId[0]} version $apiVersion-$version${if (modDescription.isEmpty()) "" else ": $modDescription"}"
    }

    companion object {
        private val userField: Field = run {
            val field = Module::class.java.getDeclaredField("user")
            field.isAccessible = true
            field
        }
    }
}

