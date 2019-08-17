package cn.tursom.treediagram.mod

@Target(AnnotationTarget.CLASS)
annotation class ModPath(vararg val paths: String)
