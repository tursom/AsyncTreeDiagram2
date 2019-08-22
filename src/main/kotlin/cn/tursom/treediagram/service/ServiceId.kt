package cn.tursom.treediagram.service

@Target(AnnotationTarget.CLASS)
annotation class ServiceId(vararg val id: String)