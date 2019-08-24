package cn.tursom.treediagram.module

@Target(AnnotationTarget.CLASS)
annotation class RequireInfo(val modId: String, val apiVersion: Int = 0, val version: Int = 0)