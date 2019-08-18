package cn.tursom.treediagram.mod

@Target(AnnotationTarget.CLASS)
annotation class RequireInfo(val modId: String, val apiVersion: Int = 0, val version: Int = 0)

operator fun RequireInfo.component1() = modId
operator fun RequireInfo.component2() = apiVersion
operator fun RequireInfo.component3() = version