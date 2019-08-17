package cn.tursom.treediagram.user

import cn.tursom.database.SqlUtils.sqlStr
import cn.tursom.database.annotation.*
import cn.tursom.treediagram.utils.Json.gson
import cn.tursom.utils.bytebuffer.fromJson

@Suppress("unused")
@TableName("users")
data class UserData(
    @PrimaryKey @NotNull val username: String,
    @NotNull val password: String,
    @NotNull @Constructor("setLevel") @FieldType("TEXT") @Getter("getLevel") val level: List<String>
) {
    fun setLevel(level: String): List<String> {
        return gson.fromJson(level)
    }

    fun getLevel(): String {
        return gson.toJson(level).sqlStr
    }
}