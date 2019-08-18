package cn.tursom.treediagram.mod

/**
 * 这个注解表示模组的绝对路径
 * 只有该模组加载为系统模组时才会采用其要求的绝对路径
 */
@Target(AnnotationTarget.CLASS)
annotation class AbsoluteModPath(vararg val paths: String)