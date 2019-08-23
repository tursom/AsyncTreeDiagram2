package cn.tursom.treediagram.manager.mod

import cn.tursom.treediagram.environment.AdminEnvironment
import cn.tursom.treediagram.environment.Environment
import cn.tursom.treediagram.environment.ModManage
import cn.tursom.treediagram.mod.ModInterface
import cn.tursom.treediagram.service.RegisterService
import cn.tursom.treediagram.service.Service
import cn.tursom.treediagram.utils.ListClassLoader
import cn.tursom.treediagram.utils.ModLoadException
import cn.tursom.utils.datastruct.async.ReadWriteLockHashMap
import cn.tursom.utils.datastruct.async.WriteLockHashMap
import cn.tursom.utils.datastruct.async.interfaces.AsyncPotableMap
import cn.tursom.web.router.SuspendRouter
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

class ModManager(
    val parentEnvironment: AdminEnvironment,
    val router: SuspendRouter<ModInterface>
) : ModManage {
    override val modManager: ModManager = this
    override val logger = Logger.getLogger("ModManager")!!
    override val systemModMap: AsyncPotableMap<String, ModInterface> = WriteLockHashMap()
    override val userModMapMap: AsyncPotableMap<String, AsyncPotableMap<String, ModInterface>> =
        WriteLockHashMap()
    private val environment: Environment = object : Environment by parentEnvironment {}

    @Volatile
    override var modEnvLastChangeTime: Long = System.currentTimeMillis()

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


    override suspend fun registerMod(user: String?, mod: ModInterface): Boolean {
        if (user != null) loadMod(user, mod)
        else loadMod(mod)
        return true
    }

    override suspend fun getSystemMod(): Set<String> {
        val idSet = HashSet<String>()
        systemModMap.forEach { (_: String, u: ModInterface) ->
            u.modId.forEach {
                idSet.add(it)
            }
            true
        }
        return idSet
    }

    override suspend fun getUserMod(user: String?): Set<String>? {
        val idSet = HashSet<String>()
        userModMapMap.get(user ?: return null)?.forEach { (_: String, u: ModInterface) ->
            u.modId.forEach {
                idSet.add(it)
            }
            true
        }
        return idSet

    }

    /**
     * 加载模组
     * 将模组的注册信息加载进系统中
     */
    private suspend fun loadMod(mod: ModInterface) {
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

        //将模组的信息加载到系统中
        mod.modId.forEach {
            systemModMap.set(it, mod)
        }

        parentEnvironment.addRouter(mod, null)

        modEnvLastChangeTime = System.currentTimeMillis()

        if (mod is Service && mod.javaClass.getAnnotation(RegisterService::class.java) != null) {
            try {
                parentEnvironment.registerService(null, mod)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 加载模组
     * 将模组的注册信息加载进系统中
     */
    suspend fun loadMod(user: String, mod: ModInterface): String {
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

        // 将模组的信息加载到系统中
        val userModMap = (userModMapMap.get(user) ?: run {
            val modMap = WriteLockHashMap<String, ModInterface>()
            userModMapMap.set(user, modMap)
            modMap
        })

        mod.modId.forEach {
            userModMap.set(it, mod)
        }

        parentEnvironment.addRouter(mod, user)

        modEnvLastChangeTime = System.currentTimeMillis()

        if (mod is Service && mod.javaClass.getAnnotation(RegisterService::class.java) != null) {
            try {
                parentEnvironment.registerService(user, mod)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return mod.modId[0]
    }

    /**
     * 卸载模组
     */
    override suspend fun removeMod(user: String?, mod: ModInterface): Boolean {
        val rUser = mod.user ?: user
        return if (rUser != null) removeUserMod(mod, rUser)
        else removeSystemMod(mod)
    }

    private suspend fun removeUserMod(mod: ModInterface, user: String): Boolean {
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
    private suspend fun removeSystemMod(mod: ModInterface): Boolean {
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
        val infoMap = HashMap<ModInterface, String>()
        systemModMap.forEach { (t, u) ->
            infoMap[u] = (infoMap[u] ?: "") + "\n|  id=$t"
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
        val infoMap = HashMap<ModInterface, String>()
        userModMapMap.get(user)?.forEach { (t, u) ->
            infoMap[u] = (infoMap[u] ?: "") + "\n|  id=$t"
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
                    val infoMap = HashMap<ModInterface, String>()
                    u.forEach { (id, mod) ->
                        infoMap[mod] = (infoMap[mod] ?: "") + "\n|  |  id=$id"
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
}

