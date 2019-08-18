package cn.tursom.treediagram.mod

/**
 * 表示一个模组需要手动处理post发送过来的数据
 */
annotation class NeedBody(val maxSize: Long = 0L)