package cn.tursom.treediagram.environment

import cn.tursom.web.HttpContent

interface UserManage {
    suspend fun registerUser(content: HttpContent): String
}