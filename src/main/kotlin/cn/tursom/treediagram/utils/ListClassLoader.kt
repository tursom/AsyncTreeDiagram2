package cn.tursom.treediagram.utils

import java.net.URL
import java.net.URLClassLoader


class ListClassLoader(url: Array<out URL>, parent: ClassLoader? = null) : URLClassLoader(url, parent) {
    private val parentList = ArrayList<ClassLoader>()
    fun addParent(parent: ClassLoader) =
        parentList.add(parent)

    override fun loadClass(name: String?): Class<*> {
        try {
            return super.loadClass(name)
        } catch (e: Throwable) {
        }
        parentList.forEach {
            try {
                return it.loadClass(name)
            } catch (e: Throwable) {
            }
        }
        throw ClassNotFoundException(name)
    }
}