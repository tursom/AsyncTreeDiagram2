package cn.tursom.treediagram.module

@Target(AnnotationTarget.CLASS)
annotation class Require(vararg val require: RequireInfo)