package cn.tursom.treediagram.mod

/**
 * 这个注解表示模组需要管理员权限
 */
@Target(AnnotationTarget.CLASS)
annotation class AdminMod(val permission: ModPermission = ModPermission.All)