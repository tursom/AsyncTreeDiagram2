package cn.tursom.treediagram

import cn.tursom.database.async.AsyncSqlAdapter
import cn.tursom.database.async.sqlite.AsyncSqliteHelper
import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.environment.ServiceEnvironment
import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.treediagram.modmanager.ModManager
import cn.tursom.treediagram.service.Service
import cn.tursom.treediagram.servicemanager.ServiceConnection
import cn.tursom.treediagram.servicemanager.ServiceConnectionDescription
import cn.tursom.treediagram.servicemanager.ServiceManager
import cn.tursom.treediagram.user.TokenData
import cn.tursom.treediagram.user.UserData
import cn.tursom.treediagram.user.UserUtils
import cn.tursom.treediagram.utils.Config
import cn.tursom.treediagram.utils.ModException
import cn.tursom.treediagram.utils.WrongTokenException
import cn.tursom.utils.asynclock.AsyncRWLockAbstractMap
import cn.tursom.utils.asynclock.ReadWriteLockHashMap
import cn.tursom.utils.background
import cn.tursom.utils.randomInt
import cn.tursom.utils.xml.Xml
import cn.tursom.web.ExceptionContent
import cn.tursom.web.HttpContent
import cn.tursom.web.HttpHandler
import cn.tursom.web.netty.NettyHttpContent
import cn.tursom.web.router.SuspendRouter
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.PrintStream
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger

class TreeDiagramHttpHandler(configPath: String = "config.xml") : HttpHandler<NettyHttpContent> {
    private val secretKey: Int = randomInt(-999999999, 999999999)
    private val systemServiceMap = ReadWriteLockHashMap<String, Service>()
    private val serviceMap = ReadWriteLockHashMap<String?, AsyncRWLockAbstractMap<String, Service>>()
    val router = SuspendRouter<ModInterface>()
    val config = run {
        val configFile = File(configPath)
        if (!configFile.exists()) {
            configFile.createNewFile()
            configFile.outputStream().use {
                it.write(Xml.toXml(Config()).toByteArray())
            }
        }
        Xml.parse<Config>(configFile)
    }

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

    val adminEnvironment: AdminEnvironment = object : AdminEnvironment {
        override val router: SuspendRouter<ModInterface> = this@TreeDiagramHttpHandler.router
        override val logger: Logger = this@TreeDiagramHttpHandler.logger
        override val modManager: ModManager get() = this@TreeDiagramHttpHandler.modManager
        override val config: Config = this@TreeDiagramHttpHandler.config
        override val fileHandler: FileHandler = this@TreeDiagramHttpHandler.fileHandler

        override suspend fun getRouterTree(): String = router.suspendToString()

        override suspend fun registerUser(content: HttpContent): String {
            return UserUtils.register(database, secretKey, content, newServer)
        }

        override suspend fun checkToken(token: String): TokenData {
            return TokenData.parseToken(token, secretKey) ?: throw WrongTokenException()
        }

        override suspend fun makeToken(user: String, password: String): String? {
            return TokenData.getToken(user, password, database = database, secretKey = secretKey)
        }

        override suspend fun getMod(user: String?, modId: String) = modManager.getMod(user, modId)

        override suspend fun registerMod(user: String?, mod: ModInterface): Boolean {
            return modManager.registerMod(user, mod)
        }

        override suspend fun removeMod(user: String?, mod: ModInterface): Boolean {
            return modManager.removeMod(user, mod)
        }

        override suspend fun registerService(user: String?, service: Service): Boolean {
            return serviceManager.registerService(user, service)
        }

        override suspend fun removeService(user: String?, service: Service): Boolean {
            return serviceManager.removeService(user, service)
        }

        override suspend fun call(user: String?, serviceId: String, message: Any?, timeout: Long): Any? {
            return serviceManager.call(user, serviceId, message, timeout)
        }

        override suspend fun connect(user: String?, serviceId: String): ServiceConnection? {
            return serviceManager.connect(user, serviceId)
        }
    }

    val environment: Environment = object : Environment by adminEnvironment {}
    val modManager = ModManager(adminEnvironment)
    val serviceManager = ServiceManager(adminEnvironment)

    init {
        logger.log(Level.INFO, "from $configPath loaded config: $config")
    }

    override fun handle(content: NettyHttpContent) = background {
        logger.log(Level.INFO, "${content.clientIp} require ${content.httpMethod} ${content.uri}")
        val (mod, path) = router.get(content.uri)
        if (mod == null) {
            logger.log(Level.WARNING, "not found ${content.clientIp} require ${content.httpMethod} ${content.uri}")
            content.responseCode = 404
            content.finish()
        } else {
            path.forEach { content.addParam(it.first, it.second) }

            try {
                if (mod.adminMod) mod.bottomHandle(content, adminEnvironment)
                else mod.bottomHandle(content, environment)
            } catch (e: ModException) {
                content.responseCode = 500
                content.reset()
                //e.printStackTrace(PrintStream(content.responseBody))
                //content.write("${e.javaClass}: ${e.message}")
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

    override fun exception(e: ExceptionContent) {
        e.cause.printStackTrace()
    }
}
