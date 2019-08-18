package cn.tursom.treediagram.mod

/**
 * 模组的相对路径，可以随着加载的用户动态的变动
 */
@Target(AnnotationTarget.CLASS)
annotation class ModPath(vararg val paths: String)
