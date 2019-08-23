package cn.tursom.treediagram

import cn.tursom.database.async.AsyncSqlAdapter
import cn.tursom.database.async.sqlite.AsyncSqliteHelper
import cn.tursom.treediagram.environment.*
import cn.tursom.treediagram.manager.mod.ModManager
import cn.tursom.treediagram.manager.router.RouterManager
import cn.tursom.treediagram.manager.service.ServiceManager
import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.treediagram.mod.ModPermission
import cn.tursom.treediagram.service.Service
import cn.tursom.treediagram.service.ServiceConnection
import cn.tursom.treediagram.user.TokenData
import cn.tursom.treediagram.user.UserData
import cn.tursom.treediagram.user.UserUtils
import cn.tursom.treediagram.utils.Config
import cn.tursom.treediagram.utils.ModException
import cn.tursom.treediagram.utils.WrongTokenException
import cn.tursom.utils.background
import cn.tursom.utils.datastruct.async.interfaces.AsyncPotableMap
import cn.tursom.utils.randomInt
import cn.tursom.utils.xml.Xml
import cn.tursom.web.HttpContent
import cn.tursom.web.HttpHandler
import cn.tursom.web.netty.NettyExceptionContent
import cn.tursom.web.netty.NettyHttpContent
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.PrintStream
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger

@Suppress("MemberVisibilityCanBePrivate")
class TreeDiagramHttpHandler(val config: Config) : HttpHandler<NettyHttpContent, NettyExceptionContent> {
    constructor(configPath: String = "config.xml") : this(File(configPath))
    constructor(configFile: File) : this({
        if (!configFile.exists()) {
            configFile.createNewFile()
            configFile.outputStream().use {
                it.write(Xml.toXml(Config()).toByteArray())
            }
        }
        Xml.parse<Config>(configFile)
    }())

    private val secretKey: Int = randomInt(-999999999, 999999999)

    private val fileHandler = run {
        if (!File(config.logPath).exists()) {
            File(config.logPath).mkdirs()
        }
        FileHandler(
            "${config.logPath}/${config.logFile}%u.%g.xml",
            config.maxLogSize,
            config.logFileCount
        )
    }

    val logger = run {
        val logger = Logger.getLogger("ModLogger")!!
        logger.addHandler(fileHandler)
        logger
    }
    val database = AsyncSqliteHelper(config.database)

    private var newServer: Boolean = runBlocking { database.select(AsyncSqlAdapter(UserData::class.java)) }.size == 0
        get() {
            return if (field) {
                field = false
                true
            } else {
                false
            }
        }
        set(value) {
            throw Exception("var newServer cannot set, $value")
        }

    val routerManager = RouterManager(logger)
    @Suppress("RedundantOverride")
    val adminEnvironment: AdminEnvironment = object : AdminEnvironment, RouterManage by routerManager {
        override val logger: Logger = this@TreeDiagramHttpHandler.logger
        override val modManager: ModManager get() = this@TreeDiagramHttpHandler.modManager
        override val config: Config = this@TreeDiagramHttpHandler.config
        override val fileHandler: FileHandler = this@TreeDiagramHttpHandler.fileHandler
        override val modEnvLastChangeTime: Long get() = modManager.modEnvLastChangeTime
        override val systemModMap: AsyncPotableMap<String, ModInterface> get() = modManager.systemModMap
        override val userModMapMap: AsyncPotableMap<String, AsyncPotableMap<String, ModInterface>> get() = modManager.userModMapMap

        override suspend fun registerUser(content: HttpContent): String {
            return UserUtils.register(database, secretKey, content, logger, this, newServer)
        }

        override suspend fun checkToken(token: String): TokenData {
            return TokenData.parseToken(token, secretKey) ?: throw WrongTokenException()
        }

        override suspend fun makeToken(user: String, password: String): String? {
            return TokenData.getToken(user, password, database = database, secretKey = secretKey)
        }

        override fun tokenStr(content: HttpContent) =
            content.getHeader("token") ?: content.getParam("token") ?: content.getCookie("token")?.value

        override suspend fun makeGuestToken(): String {
            return TokenData.getGuestToken(secretKey)
        }

        override suspend fun getMod(user: String?, modId: String) = modManager.getMod(user, modId)

        override suspend fun getSystemMod(): Set<String> = modManager.getSystemMod()
        override suspend fun getUserMod(user: String?): Set<String>? = modManager.getUserMod(user)
        override suspend fun getModTree(user: String?): String = modManager.getModTree(user)

        override suspend fun registerMod(user: String?, mod: ModInterface): Boolean = modManager.registerMod(user, mod)
        override suspend fun removeMod(user: String?, mod: ModInterface): Boolean = modManager.removeMod(user, mod)

        override suspend fun registerService(user: String?, service: Service): Boolean {
            return serviceManager.registerService(user, service)
        }

        override suspend fun removeService(user: String?, service: Service): Boolean {
            return serviceManager.removeService(user, service)
        }

        override suspend fun call(user: String?, serviceId: String, message: Any?, timeout: Long): Any? {
            return serviceManager.call(user, serviceId, message, timeout)
        }

        override suspend fun getCaller(user: String?, serviceId: String): ServiceCaller? {
            return serviceManager.getCaller(user, serviceId)
        }

        override suspend fun connect(user: String?, serviceId: String): ServiceConnection? {
            return serviceManager.connect(user, serviceId)
        }
    }
    val serviceManager = ServiceManager(adminEnvironment)
    val modManager = ModManager(adminEnvironment, routerManager.router)

    val environment: Environment = object : Environment by adminEnvironment {}
    val adminModEnvironment: AdminModEnvironment = object : AdminModEnvironment by adminEnvironment {}
    val adminRouterEnvironment: AdminRouterEnvironment = object : AdminRouterEnvironment by adminEnvironment {}
    val adminServiceEnvironment: AdminServiceEnvironment = object : AdminServiceEnvironment by adminEnvironment {}
    val adminUserEnvironment = object : AdminUserEnvironment by adminEnvironment {}

    override fun handle(content: NettyHttpContent) = background {
        logger.log(Level.INFO, "${content.clientIp} require ${content.httpMethod} ${content.uri}")
        val (mod, path) = routerManager.router.get(content.uri)
        if (mod == null) {
            logger.log(Level.WARNING, "not found ${content.clientIp} require ${content.httpMethod} ${content.uri}")
            content.responseCode = 404
            content.finish()
        } else {
            path.forEach { content.addParam(it.first, it.second) }

            try {
                when (mod.modPermission) {
                    ModPermission.All -> mod.bottomHandle(content, adminEnvironment)
                    ModPermission.ModManage -> mod.bottomHandle(content, adminModEnvironment)
                    ModPermission.RouterManage -> mod.bottomHandle(content, adminRouterEnvironment)
                    ModPermission.ServiceManage -> mod.bottomHandle(content, adminServiceEnvironment)
                    ModPermission.UserManage -> mod.bottomHandle(content, adminUserEnvironment)
                    else -> mod.bottomHandle(content, environment)
                }
            } catch (e: ModException) {
                content.responseCode = 500
                content.reset()
                logger.log(Level.WARNING, "${e.javaClass}: ${e.message}")
                content.responseCode = 500
                content.reset()
                e.printStackTrace(PrintStream(content.responseBody))
                content.finish()
            } catch (e: Throwable) {
                e.printStackTrace()
                content.responseCode = 500
                content.reset()
                e.printStackTrace(PrintStream(content.responseBody))
                content.finish()
            }
        }
    }
}
