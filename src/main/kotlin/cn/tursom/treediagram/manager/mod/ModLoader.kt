package cn.tursom.treediagram.manager.mod

import cn.tursom.treediagram.environment.ModManage
import cn.tursom.treediagram.module.IModule
import cn.tursom.treediagram.utils.ListClassLoader
import cn.tursom.utils.AsyncHttpRequest
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.jar.JarFile


/**
 * 用于加载模组
 * 模组可由网络或者本地加载
 * 亦可将配置写入一个文件中
 * 会优先尝试从本地加载模组
 * 本地文件不存在则会从网络加载模组
 */
class ModLoader private constructor(
    private val user: String? = null,
    private val modManager: ModManage,
    private val className: List<String>,
    private val classLoader: ClassLoader
) {
    /**
     * 手动加载模组
     * @return 是否所有的模组都加载成功
     */
    suspend fun load(): List<String> {
        val loadedMod = ArrayList<String>()
        className.forEach { className ->
            try {
                loadSingleMod(className, loadedMod)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return loadedMod
    }

    private suspend fun loadSingleMod(className: String, loadedMod: ArrayList<String>? = null) {
        try {
            //获取一个指定模组的对象
            val modClass = classLoader.loadClass(className)
            if (!IModule::class.java.isAssignableFrom(modClass)) return
            val modObject = try {
                modClass.newInstance() as IModule
            } catch (e: Exception) {
                return
            }
            //加载模组
            modManager.registerMod(user, modObject)
            loadedMod?.add(className)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    companion object {
        val parentLoader: Field = ClassLoader::class.java.getDeclaredField("parent")

        init {
            parentLoader.isAccessible = true
        }

        @JvmStatic
        fun getClassName(jarPath: String): List<String> {
            val myClassName = ArrayList<String>()
            for (entry in JarFile(jarPath).entries()) {
                val entryName = entry.name
                if (entryName.endsWith(".class")) {
                    myClassName.add(entryName.replace("/", ".").substring(0, entryName.lastIndexOf(".")))
                }
            }
            return myClassName
        }

        suspend fun getModLoader(
            configData: ClassData,
            user: String? = null,
            rootPath: String? = null,
            loadInstantly: Boolean = false,
            modManager: ModManage,
            parentClassLoader: ClassLoader = Thread.currentThread().contextClassLoader
        ): ModLoader {
            val file = if (rootPath == null) {
                configData.path
            } else {
                rootPath + configData.path
            }
            val jarFile = if (file != null && !File(file).exists()) {
                val localJarPath = configData.url!!.split('?')[0].split('/').last()
                val localJarFile = File(localJarPath)
                if (!localJarFile.exists()) localJarFile.outputStream().use {
                    it.write(AsyncHttpRequest.get(configData.url).body()!!.bytes())
                }
                localJarPath
            } else {
                file
            }!!
            val classLoader = ListClassLoader(arrayOf(File(jarFile).toURI().toURL()), parentClassLoader)
            val classList = configData.classname ?: getClassName(jarFile)
            val loader = ModLoader(user, modManager, classList, classLoader)
            if (loadInstantly) {
                loader.load()
            }
            return loader
        }
    }
}

