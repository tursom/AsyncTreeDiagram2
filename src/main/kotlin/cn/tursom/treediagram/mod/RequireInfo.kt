package cn.tursom.treediagram.mod

@Target(AnnotationTarget.CLASS)
annotation class RequireInfo(val modId: String, val apiVersion: Int = 0, val version: Int = 0)