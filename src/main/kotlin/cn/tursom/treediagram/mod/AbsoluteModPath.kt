package cn.tursom.treediagram.mod

@Target(AnnotationTarget.CLASS)
annotation class AbsoluteModPath(vararg val paths: String)