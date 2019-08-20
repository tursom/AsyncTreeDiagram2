package cn.tursom.treediagram.user

import cn.tursom.database.SqlUtils.sqlStr
import cn.tursom.database.annotation.*
import cn.tursom.treediagram.utils.Json.gson
import cn.tursom.utils.fromJson

@Suppress("unused")
@TableName("users")
data class UserData(
    @PrimaryKey @NotNull val username: String,
    @NotNull val password: String,
    @NotNull @Constructor("toSetLevel") @FieldType("TEXT") @Getter("toGetLevel") val level: List<String>
) {
    fun toSetLevel(level: String): List<String> = gson.fromJson(level)
    fun toGetLevel() = gson.toJson(level).sqlStr
}