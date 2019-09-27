package cn.tursom.treediagram.manager.mod

import cn.tursom.aop.ProxyHandler
import cn.tursom.aop.aspect.Aspect
import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.environment.ModManage
import cn.tursom.treediagram.module.IModule
import cn.tursom.treediagram.service.RegisterService
import cn.tursom.treediagram.service.Service
import cn.tursom.treediagram.utils.ListClassLoader
import cn.tursom.treediagram.utils.ModLoadException
import cn.tursom.utils.cache.DefaultAsyncPotableCacheMap
import cn.tursom.utils.cache.interfaces.AsyncPotableCacheMap
import cn.tursom.utils.datastruct.async.ReadWriteLockHashMap
import cn.tursom.utils.datastruct.async.WriteLockHashMap
import cn.tursom.utils.datastruct.async.collections.AsyncMapSet
import cn.tursom.utils.datastruct.async.interfaces.AsyncPotableMap
import cn.tursom.utils.datastruct.async.interfaces.AsyncPotableSet
import cn.tursom.utils.datastruct.async.interfaces.AsyncSet
import cn.tursom.web.router.SuspendRouter
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

class ModManager(
    val parentEnvironment: AdminEnvironment,
    val router: SuspendRouter<IModule>
) : ModManage {
    override val modManager: ModManager = this
    override val logger = Logger.getLogger("ModManager")!!
    override val systemModMap: AsyncPotableCacheMap<String, IModule> = DefaultAsyncPotableCacheMap(5)
    override val userModMapMap: AsyncPotableCacheMap<String, AsyncPotableMap<String, IModule>> =
        DefaultAsyncPotableCacheMap(5)
    private val naturalSystemModMap: AsyncPotableCacheMap<String, IModule> = DefaultAsyncPotableCacheMap(5)
    private val naturalUserModMapMap: AsyncPotableCacheMap<String, AsyncPotableMap<String, IModule>> =
        DefaultAsyncPotableCacheMap(5)
    override val aspectMap: AsyncPotableCacheMap<String?, AsyncPotableSet<Aspect>> = DefaultAsyncPotableCacheMap(5)
    private val environment: Environment = object : Environment by parentEnvironment {}

    @Volatile
    override var modEnvLastChangeTime: Long = System.currentTimeMillis()

    init {
        // 加载系统模组
        runBlocking {
            arrayOf<IModule>(
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
                cn.tursom.treediagram.systemmod.HtmlIndex(),
                cn.tursom.treediagram.systemmod.Router(),
                cn.tursom.treediagram.systemmod.GuestLogin()
            ).forEach {
                loadMod(it)
            }
        }
    }

    override suspend fun getMod(user: String?, modId: String) = if (user != null) {
        modManager.userModMapMap.get(user)?.get(modId)
    } else {
        this.modManager.systemModMap.get(modId)
    }


    override suspend fun registerMod(user: String?, mod: IModule): Boolean {
        if (user != null) loadMod(user, mod)
        else loadMod(mod)
        return true
    }

    override suspend fun getSystemMod(): Set<String> {
        val idSet = HashSet<String>()
        systemModMap.forEach { (_: String, u: IModule) ->
            u.modId.forEach {
                idSet.add(it)
            }
            true
        }
        return idSet
    }

    override suspend fun getUserMod(user: String?): Set<String>? {
        val idSet = HashSet<String>()
        userModMapMap.get(user ?: return null)?.forEach { (_: String, u: IModule) ->
            u.modId.forEach {
                idSet.add(it)
            }
            true
        }
        return idSet

    }

    private suspend fun enhanceMod(mod: IModule, aspectSet: AsyncSet<Aspect>): IModule {
        var enhancedMod = mod
        aspectSet.forEach {
            if (it.pointcut.matchClass(mod.javaClass)) {
                enhancedMod = ProxyHandler.proxyEnhance(enhancedMod, it) as IModule
            }
            true
        }
        return enhancedMod
    }

    /**
     * 加载模组
     * 将模组的注册信息加载进系统中
     */
    private suspend fun loadMod(mod: IModule) {
        //输出日志信息
        logger.info("loading mod: ${mod.javaClass}, mod permission: ${mod.modPermission}")

        mod.require?.forEach {
            val parentMod = systemModMap.get(it.modId) ?: throw ModLoadException("mod ${it.modId} mot found")
            if (parentMod.apiVersion != it.apiVersion)
                throw ModLoadException("mod ${it.modId} api version is ${parentMod.apiVersion}, but ${mod.modId} require ${it.apiVersion}")
            if (parentMod.version < it.version)
                throw ModLoadException("mod ${it.modId} version is ${parentMod.version}, but ${mod.modId} require ${it.version}")
            (mod.javaClass.classLoader as ListClassLoader).addParent(parentMod.javaClass.classLoader)
        }

        //记得销毁被替代的模组
        removeSystemMod(mod)

        //调用模组的初始化函数
        mod.init(null, parentEnvironment)

        parentEnvironment.addRouter(mod, null)

        modEnvLastChangeTime = System.currentTimeMillis()

        if (mod is Service && mod.javaClass.getAnnotation(RegisterService::class.java) != null) {
            try {
                parentEnvironment.registerService(null, mod)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val aspectSet = getAspectSet(null)
        val enhancedMod = enhanceMod(mod, aspectSet)

        //将模组的信息加载到系统中
        mod.modId.forEach {
            systemModMap.set(it, enhancedMod)
            naturalSystemModMap.set(it, enhancedMod)
        }
    }

    /**
     * 加载模组
     * 将模组的注册信息加载进系统中
     */
    private suspend fun loadMod(user: String, mod: IModule): String {
        // 输出日志信息
        logger.info("user: $user loading mod: ${mod.javaClass}, mod permission: ${mod.modPermission}")

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
        mod.init(user, parentEnvironment)

        parentEnvironment.addRouter(mod, user)

        modEnvLastChangeTime = System.currentTimeMillis()

        if (mod is Service && mod.javaClass.getAnnotation(RegisterService::class.java) != null) {
            try {
                parentEnvironment.registerService(user, mod)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val aspectSet = getAspectSet(user)
        val enhancedMod = enhanceMod(mod, aspectSet)

        // 将模组的信息加载到系统中
        val userModMap = userModMapMap.get(user) { WriteLockHashMap() }
        val naturalUserModMap = naturalUserModMapMap.get(user) { WriteLockHashMap() }

        mod.modId.forEach {
            userModMap.set(it, enhancedMod)
            naturalUserModMap.set(it, mod)
        }

        return mod.modId[0]
    }

    /**
     * 卸载模组
     */
    override suspend fun removeMod(user: String?, mod: IModule): Boolean {
        val rUser = mod.user ?: user
        return if (rUser != null) removeUserMod(mod, rUser)
        else removeSystemMod(mod)
    }

    private suspend fun removeUserMod(mod: IModule, user: String): Boolean {
        logger.info("user $user try remove mod: $mod")

        val userModMap = userModMapMap.get(user) ?: return false
        logger.info("user $user remove mod: $mod")
        try {
            mod.destroy(environment)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mod.modId.forEach {
            if (mod === userModMap.get(it))
                userModMap.remove(it)
        }

        parentEnvironment.removeRouter(mod, user)

        modEnvLastChangeTime = System.currentTimeMillis()

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
    private suspend fun removeSystemMod(mod: IModule): Boolean {
        logger.info("remove system mod: ${mod.javaClass}")
        try {
            mod.destroy(parentEnvironment)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mod.modId.forEach {
            if (mod === systemModMap.get(it)) systemModMap.remove(it)
        }

        parentEnvironment.removeRouter(mod, null)

        modEnvLastChangeTime = System.currentTimeMillis()

        if (mod is Service) {
            try {
                parentEnvironment.removeService(null, mod)
            } catch (e: Exception) {
            }
        }
        return true
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
        if (modEnvLastChangeTime < systemTreeCacheTime) {
            return systemTreeCache
        }
        val sb = StringBuilder()
        sb.append("system\n")
        val infoMap = HashMap<IModule, String>()
        systemModMap.forEach { (t, u) ->
            infoMap[u] = (infoMap[u] ?: "") + "\n|  id = $t"
            true
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
            if (time > modEnvLastChangeTime) {
                return cache
            }
        }
        sb.append("$user\n")
        val infoMap = HashMap<IModule, String>()
        userModMapMap.get(user)?.forEach { (t, u) ->
            infoMap[u] = (infoMap[u] ?: "") + "\n|  id = $t"
            true
        }
        infoMap.forEach { (t, u) ->
            sb.append("|- $t$u\n")
        }
        val str = sb.toString()
        userCache.set(user, System.currentTimeMillis() to str)
        return str
    }


    override suspend fun getModTree(user: String?): String {
        return when (user) {
            null -> {
                if (modEnvLastChangeTime < cacheTime) return cache

                val sb = StringBuilder()
                sb.append(getSystemTree())
                if (userModMapMap.isNotEmpty()) sb.append("user\n")
                userModMapMap.forEach { (t, u) ->
                    sb.append("|- $t\n")
                    val infoMap = HashMap<IModule, String>()
                    u.forEach { (id, mod) ->
                        infoMap[mod] = (infoMap[mod] ?: "") + "\n|  |  id = $id"
                        true
                    }
                    infoMap.forEach { (t, u) ->
                        sb.append("|  |- $t$u\n")
                    }
                    true
                }
                cache = sb.toString()
                cacheTime = System.currentTimeMillis()
                cache
            }
            "system" -> getSystemTree()
            else -> getUserTree(user)
        }
    }

    private suspend fun getModMap(user: String?): AsyncPotableMap<String, IModule>? {
        return if (user == null) systemModMap
        else {
            userModMapMap.get(user)
        }
    }

    private suspend fun getNaturalModMap(user: String?): AsyncPotableMap<String, IModule>? {
        return if (user == null) naturalSystemModMap
        else {
            naturalUserModMapMap.get(user)
        }
    }

    private suspend fun getAspectSet(user: String?): AsyncPotableSet<Aspect> = aspectMap.get(user) { AsyncMapSet() }

    override suspend fun addAspect(user: String?, aspect: Aspect) {
        val aspectSet = getAspectSet(user)
        if (!aspectSet.contains(aspect)) {
            val modMap = getModMap(user)
            modMap?.forEach { (id, mod) ->
                val topMod = ProxyHandler.getTopBean(mod)
                if (aspect.pointcut.matchClass(topMod.javaClass)) {
                    modMap.set(id, ProxyHandler.proxyEnhance(mod, aspect) as IModule)
                }
                true
            }
            aspectSet.put(aspect)
        }
    }

    override suspend fun removeAspect(user: String?, aspect: Aspect) {
        val aspectSet = aspectMap.get(user) ?: return
        aspectSet.remove(aspect)
        val modMap = getModMap(user) ?: return
        val naturalMap = getNaturalModMap(user) ?: return

        modMap.clear()
        naturalMap.forEach { (id, mod) ->
            modMap.set(id, enhanceMod(mod, aspectSet))
            true
        }
    }
}

