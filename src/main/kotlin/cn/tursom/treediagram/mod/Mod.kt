package cn.tursom.treediagram.mod

import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.service.AdminService
import cn.tursom.treediagram.service.Service
import java.io.File
import java.lang.reflect.Field
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Level
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class Mod : ModInterface, Service {
    override val user: String? = null
    //override val adminMod: Boolean = super.adminMod
    override val require = javaClass.getAnnotation(Require::class.java)?.require
    override val version: Int = javaClass.getAnnotation(Version::class.java)?.version ?: 0
    override val apiVersion: Int = javaClass.getAnnotation(ApiVersion::class.java)?.version ?: 0
    override val adminService: Boolean = javaClass.getAnnotation(AdminService::class.java) != null

    /**
     * 模组私有目录
     * 在调用的时候会自动创建目录，不必担心目录不存在的问题
     * 如果有模组想储存文件请尽量使用这个目录
     */
    val modPath by lazy {
        val path = "mods/${this::class.java.name}/"
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
        environment.logger.log(Level.INFO, "mod $modId init by $user")
    }

    /**
     * 当模组生命周期结束时被调用
     */
    override suspend fun destroy(environment: Environment) {
        environment.logger.log(Level.INFO, "mod $modId destroy")
    }

    /** service 生命周期 */

    override suspend fun initService(user: String?, environment: Environment) {
        environment.logger.log(Level.INFO, "service $serviceId init")
    }

    override suspend fun destroyService(environment: Environment) {
        environment.logger.log(Level.INFO, "service $serviceId destroy")
    }

    /** utils */

    override fun toString(): String {
        return "$modId version $apiVersion-$version${if (modDescription.isEmpty()) "" else ": $modDescription"}"
    }

    companion object {
        private val userField: Field = run {
            val field = Mod::class.java.getDeclaredField("user")
            field.isAccessible = true
            field
        }

        @JvmStatic
        val uploadRootPath = "upload/"

        @JvmStatic
        fun getUploadPath(user: String) = "$uploadRootPath$user/"

        @JvmStatic
        val fileThreadPool: ExecutorService = Executors.newSingleThreadExecutor()

        @JvmStatic
        suspend fun readFile(file: File): ByteArray {
            return suspendCoroutine { cont ->
                fileThreadPool.execute {
                    cont.resume(file.readBytes())
                }
            }
        }
    }
}

