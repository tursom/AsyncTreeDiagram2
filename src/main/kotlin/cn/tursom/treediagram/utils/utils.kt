package cn.tursom.treediagram.utils

import cn.tursom.web.HttpContent

val HttpContent.token: String? get() = getHeader("token") ?: getParam("token") ?: getCookie("token")