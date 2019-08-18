package cn.tursom.treediagram.mod

@Target(AnnotationTarget.CLASS)
annotation class Require(vararg val require: RequireInfo)