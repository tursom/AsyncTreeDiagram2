package cn.tursom.treediagram.modloader

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.treediagram.service.RegisterService
import cn.tursom.treediagram.service.Service
import cn.tursom.treediagram.utils.ListClassLoader
import cn.tursom.treediagram.utils.ModLoadException
import cn.tursom.utils.asynclock.AsyncRWLockAbstractMap
import cn.tursom.utils.asynclock.ReadWriteLockHashMap
import cn.tursom.utils.asynclock.WriteLockHashMap
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

class ModManager(val parentEnvironment: AdminEnvironment) : AdminEnvironment by parentEnvironment {
    override val logger = Logger.getLogger("ModManager")!!
    val systemModMap = WriteLockHashMap<String, ModInterface>()
    val userModMapMap: AsyncRWLockAbstractMap<String, AsyncRWLockAbstractMap<String, ModInterface>> = WriteLockHashMap()

    @Volatile
    var lastChangeTime: Long = System.currentTimeMillis()

    private val adminEnvironment: AdminEnvironment = object : AdminEnvironment by parentEnvironment {
        override val modManager: ModManager get() = this@ModManager

        override suspend fun registerMod(user: String?, mod: ModInterface): Boolean {
            if (user != null) loadMod(user, mod)
            else loadMod(mod)
            return true
        }

        override suspend fun removeMod(user: String?, mod: ModInterface): Boolean {
            return true
        }
    }

    private val environment: Environment = object : Environment by parentEnvironment {}

    override suspend fun registerMod(user: String?, mod: ModInterface): Boolean {
        if (user != null) loadMod(user, mod)
        else loadMod(mod)
        return true
    }

    init {
        // 加载系统模组
        runBlocking {
            arrayOf<ModInterface>(
                cn.tursom.treediagram.systemmod.Echo(),
                cn.tursom.treediagram.systemmod.Email(),
                cn.tursom.treediagram.systemmod.GroupEmail(),
                cn.tursom.treediagram.systemmod.MultipleEmail(),
                cn.tursom.treediagram.systemmod.ModLoader(),
                cn.tursom.treediagram.systemmod.Upload(),
                cn.tursom.treediagram.systemmod.GetUploadFileList(),
                cn.tursom.treediagram.systemmod.Register(),
                cn.tursom.treediagram.systemmod.Login(),
                cn.tursom.treediagram.systemmod.Close(),
                cn.tursom.treediagram.systemmod.LoadedMod(),
                cn.tursom.treediagram.systemmod.RouterTree(),
                cn.tursom.treediagram.systemmod.Help(),
                cn.tursom.treediagram.systemmod.ModTree(),
                cn.tursom.treediagram.systemmod.ModRemover(),
                cn.tursom.treediagram.systemmod.Download(),
                cn.tursom.treediagram.systemmod.AutoLoadMod(),
                cn.tursom.treediagram.systemmod.HtmlIndex()
            ).forEach {
                loadMod(it)
            }
        }
    }

    suspend fun getSystemMod(): Set<String> {
        val pathSet = HashSet<String>()
        systemModMap.forEach { _: String, u: ModInterface ->
            u.routeList.forEach {
                pathSet.add(it)
            }
        }
        return pathSet
    }

    suspend fun getUserMod(user: String?): Set<String>? {
        val pathSet = HashSet<String>()
        userModMapMap.get(user ?: return null)?.forEach { _: String, u: ModInterface ->
            u.routeList.forEach {
                pathSet.add(it)
            }
        }
        return pathSet

    }

    suspend fun findMod(modName: String, user: String? = null): ModInterface? {
        return if (user != null) {
            (userModMapMap.get(user) ?: return null).get(modName)
        } else {
            systemModMap.get(modName)
        }
    }

    /**
     * 加载模组
     * 将模组的注册信息加载进系统中
     */
    internal suspend fun loadMod(mod: ModInterface) {
        //输出日志信息
        logger.info("loading mod: $mod")

        mod.require?.forEach {
            val parentMod = systemModMap.get(it.modId) ?: throw ModLoadException("mod ${it.modId} mot found")
            if (parentMod.apiVersion != it.apiVersion)
                throw ModLoadException("mod ${it.modId} api version is ${parentMod.apiVersion}, but ${mod.modId} require ${it.apiVersion}")
            if (parentMod.version < it.version)
                throw ModLoadException("mod ${it.modId} version is ${parentMod.version}, but ${mod.modId} require ${it.version}")
            (mod.javaClass.classLoader as ListClassLoader).addParent(parentMod.javaClass.classLoader)
        }

        //记得销毁被替代的模组
        removeMod(mod)

        //调用模组的初始化函数
        mod.init(null, adminEnvironment)

        //将模组的信息加载到系统中
        systemModMap.set(mod.modId, mod)
        systemModMap.set(mod.modId.split('.').last(), mod)


        mod.routeList.forEach {
            addRoute("/mod/system/$it", mod)
        }

        mod.absRouteList.forEach {
            addRoute("/$it", mod)
        }

        lastChangeTime = System.currentTimeMillis()

        if (mod is Service && mod.javaClass.getAnnotation(RegisterService::class.java) != null) {
            try {
                parentEnvironment.registerService(null, mod)
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 加载模组
     * 将模组的注册信息加载进系统中
     */
    suspend fun loadMod(user: String, mod: ModInterface): String {
        // 输出日志信息
        logger.info("user: $user loading mod: ${mod::class.java.name}")

        mod.require?.forEach {
            val parentMod = systemModMap.get(it.modId) ?: userModMapMap.get(user)?.get(it.modId)
            ?: throw ModLoadException("mod ${it.modId} mot found")
            if (parentMod.apiVersion != it.apiVersion)
                throw ModLoadException("mod ${it.modId} api version is ${parentMod.apiVersion}, but ${mod.modId} require ${it.apiVersion}")
            if (parentMod.version < it.version)
                throw ModLoadException("mod ${it.modId} version is ${parentMod.version}, but ${mod.modId} require ${it.version}")
            (mod.javaClass.classLoader as ListClassLoader).addParent(parentMod.javaClass.classLoader)
        }

        // 记得销毁被替代的模组
        removeMod(user, mod)

        // 调用模组的初始化函数
        mod.init(user, adminEnvironment)

        // 将模组的信息加载到系统中
        val userModMap = (userModMapMap.get(user) ?: run {
            val modMap = WriteLockHashMap<String, ModInterface>()
            userModMapMap.set(user, modMap)
            modMap
        })

        userModMap.set(mod.modId, mod)
        userModMap.set(mod.simpModId, mod)

        mod.routeList.forEach {
            addRoute("/mod/user/$user/$it", mod)
            addRoute("/user/$user/$it", mod)
        }

        lastChangeTime = System.currentTimeMillis()

        if (mod is Service && mod.javaClass.getAnnotation(RegisterService::class.java) != null) {
            try {
                parentEnvironment.registerService(user, mod)
            } catch (e: Exception) {
            }
        }

        return mod.modId
    }

    /**
     * 卸载模组
     */
    override suspend fun removeMod(user: String?, mod: ModInterface): Boolean {
        if (user == null) {
            removeMod(mod)
            return true
        }
        logger.info("user $user try remove mod: $mod")

        val userModMap = userModMapMap.get(user) ?: return false
        val modObject = userModMap.get(mod.modId) ?: return false
        logger.info("user $user remove mod: $mod")
        try {
            modObject.destroy(environment)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (userModMap.contains(modObject.modId)) {
            userModMap.remove(modObject.modId)
            if (modObject === userModMap.get(modObject.modId.split('.').last()))
                userModMap.remove(modObject.modId.split('.').last())
        }
        modObject.routeList.forEach {
            val route = "/mod/user/$user/$it"
            logger.info("try delete route $route")
            if (modObject === router.get(route).first) {
                logger.info("delete route $route")
                router.delRoute(route)
            }
        }
        lastChangeTime = System.currentTimeMillis()

        if (mod is Service) {
            try {
                parentEnvironment.removeService(user, mod)
            } catch (e: Exception) {
            }
        }
        return true
    }

    /**
     * 卸载模组
     */
    suspend fun removeMod(mod: ModInterface) {
        logger.info("try remove system mod: $mod")
        val modObject = systemModMap.get(mod.modId) ?: return
        logger.info("remove system mod: $mod")
        try {
            modObject.destroy(adminEnvironment)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (systemModMap.contains(modObject.modId)) {
            systemModMap.remove(modObject.modId)
            if (modObject === systemModMap.get(modObject.modId.split('.').last()))
                systemModMap.remove(modObject.modId.split('.').last())
        }
        modObject.routeList.forEach {
            val route = "/mod/system/$it"
            logger.info("try delete route $route")
            if (modObject === router.get(route).first) {
                logger.info("delete route $route")
                router.delRoute(route)
            }
        }
        lastChangeTime = System.currentTimeMillis()

        if (mod is Service) {
            try {
                parentEnvironment.removeService(null, mod)
            } catch (e: Exception) {
            }
        }
    }

    private suspend fun addRoute(fullRoute: String, mod: ModInterface) {
        router.set(fullRoute, mod)
    }

    // 模组树部分
    @Volatile
    private var cacheTime: Long = 0
    @Volatile
    private var cache: String = ""
    private var userCache = ReadWriteLockHashMap<String, Pair<Long, String>>()
    @Volatile
    private var systemTreeCacheTime: Long = 0
    @Volatile
    private var systemTreeCache: String = ""

    private suspend fun getSystemTree(): String {
        if (lastChangeTime < systemTreeCacheTime) {
            return systemTreeCache
        }
        val sb = StringBuilder()
        sb.append("system\n")
        val infoMap = HashMap<ModInterface, String>()
        systemModMap.forEach { t, u ->
            infoMap[u] = (infoMap[u] ?: "") + "\n|  id=$t"
        }
        infoMap.forEach { (t, u) ->
            sb.append("|- $t$u\n")
        }
        systemTreeCache = sb.toString()
        systemTreeCacheTime = System.currentTimeMillis()
        return systemTreeCache
    }

    private suspend fun getUserTree(user: String): String {
        val sb = StringBuilder()
        val cachePair = userCache.get(user)
        if (cachePair != null) {
            val (time, cache) = cachePair
            if (time > lastChangeTime) {
                return cache
            }
        }
        sb.append("$user\n")
        val infoMap = HashMap<ModInterface, String>()
        userModMapMap.get(user)?.forEach { t, u ->
            infoMap[u] = (infoMap[u] ?: "") + "\n|  id=$t"
        }
        infoMap.forEach { (t, u) ->
            sb.append("|- $t$u\n")
        }
        val str = sb.toString()
        userCache.set(user, System.currentTimeMillis() to str)
        return str
    }


    suspend fun getModTree(user: String?): String {
        return when (user) {
            null -> {
                if (lastChangeTime < cacheTime) return cache

                val sb = StringBuilder()
                sb.append(getSystemTree())
                if (userModMapMap.isNotEmpty()) sb.append("user\n")
                userModMapMap.forEach { t, u ->
                    sb.append("|- $t\n")
                    val infoMap = HashMap<ModInterface, String>()
                    u.forEach { id, mod ->
                        infoMap[mod] = (infoMap[mod] ?: "") + "\n|  |  id=$id"
                    }
                    infoMap.forEach { (t, u) ->
                        sb.append("|  |- $t$u\n")
                    }
                }
                cache = sb.toString()
                cacheTime = System.currentTimeMillis()
                cache
            }
            "system" -> getSystemTree()
            else -> getUserTree(user)
        }
    }
}

