package cn.tursom.treediagram.user

import cn.tursom.database.async.AsyncSqlHelper
import cn.tursom.treediagram.user.UserUtils.findUser
import cn.tursom.treediagram.user.UserUtils.tryLogin
import cn.tursom.treediagram.utils.Json.gson
import cn.tursom.treediagram.utils.ModException
import cn.tursom.utils.base64
import cn.tursom.utils.base64decode
import cn.tursom.utils.digest
import cn.tursom.utils.md5
import cn.tursom.web.HttpContent

/**
 * token的结构为
 * token = head.body.签名
 * 其中
 * head = base64(加密方式)
 * body = base64(TokenData json序列化)
 * 随机数 = 一至十位随机整数，程序启动时确定
 * 秘钥 = md5(随机数)
 * 签名 = 加密方式(head.body.秘钥)
 *
 * 由于秘钥是在启动时随机得出的，故所有token都会在服务器重启后失效
 */

@Suppress("DataClassPrivateConstructor")
data class TokenData private constructor(
    val usr: String?,  //用户名
    val lev: List<String>?, //用户权限
    val tim: Long? = System.currentTimeMillis(),  //签发时间
    val exp: Long? = 1000 * 60 * 60 * 24 * 3  //过期时间
) {

    /**
     * 签发一个token
     * @param username 用户名
     * @return 一整个token
     */
    internal fun getToken(
        secretKey: Int
    ): String {
        val body = "$digestFunctionBase64.${gson.toJson(this).base64()}"
        return "$body.${"$body.$secretKey".md5()}"
    }

    internal fun getToken(
        digestType: String,
        secretKey: Int
    ): String {
        val body = "${digestType.base64()}.${gson.toJson(this).base64()}"
        return "$body.${"$body.$secretKey".digest(digestType)}"
    }

    companion object {
        val digestFunctionBase64 = "MD5".base64()  //默认md5加密

        fun getGuestToken(
            secretKey: Int,
            exp: Long? = 1000 * 60 * 60 * 24 * 3
        ): String {
            return TokenData(null, listOf("guest")).getToken(secretKey)
        }

        /**
         * 签发一个token
         * @param username 用户名
         * @return 一整个token
         */
        suspend fun getToken(
            username: String,
            password: String,
            exp: Long? = 1000 * 60 * 60 * 24 * 3,
            database: AsyncSqlHelper,
            secretKey: Int
        ): String? {
            return if (tryLogin(database, username, password)) {
                val body = "$digestFunctionBase64.${gson.toJson(TokenData(username, run {
                    val user = findUser(database, username)
                    user?.level ?: listOf("user")
                }, exp = exp)).base64()}"
                "$body.${"$body.$secretKey".md5()}"
            } else {
                null
            }
        }

        suspend fun getToken(
            username: String,
            password: String,
            exp: Long? = 1000 * 60 * 60 * 24 * 3,
            database: AsyncSqlHelper,
            secretKey: Int,
            digestType: String
        ): String? {
            return if (tryLogin(database, username, password)) {
                val body = "${digestType.base64()}.${gson.toJson(TokenData(username, run {
                    val user = findUser(database, username)
                    user?.level ?: listOf("user")
                }, exp = exp)).base64()}"
                "$body.${"$body.$secretKey".digest(digestType)}"
            } else {
                null
            }
        }

        /**
         * 验证一个token
         *
         * @param token 需要验证的整个token
         * @return 验证结果，如果失败返回null，成功则返回一个TokenData对象
         */
        fun parseToken(token: String, secretKey: Int): TokenData? {
            val data = token.split('.')
            return when {
                data.size != 3 -> null
                "${data[0]}.${data[1]}.$secretKey".digest(data[0].base64decode()) == data[2] -> try {
                    gson.fromJson(data[1].base64decode(), TokenData::class.java)
                } catch (e: Exception) {
                    null
                }
                else -> null
            }
        }

        fun parseToken(content: HttpContent, secretKey: Int): TokenData {
            val tokenStr = content.getHeader("token") ?: content.getParam("token") ?: throw ModException("no token get")
            return parseToken(tokenStr, secretKey) ?: throw ModException("no token get")
        }
    }
}