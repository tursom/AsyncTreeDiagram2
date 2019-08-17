package cn.tursom.treediagram.user

import cn.tursom.database.async.AsyncSqlAdapter
import cn.tursom.database.async.AsyncSqlHelper
import cn.tursom.database.clauses.clause
import cn.tursom.utils.sha256
import cn.tursom.web.HttpContent

object UserUtils{
    suspend fun findUser(database: AsyncSqlHelper, username: String): UserData? {
        val adapter = AsyncSqlAdapter(UserData::class.java)
        database.select(adapter, null, where = clause {
            !UserData::username equal !username
        }, maxCount = 1)
        return if (adapter.count() == 0) null
        else adapter[0]
    }

    suspend fun tryLogin(database: AsyncSqlHelper, username: String, password: String): Boolean {
        //查询用户数据
        val userData = findUser(database, username)
        return "$username$password$username$password".sha256() == userData?.password
    }

    /**
     * 添加一个新用户
     * @param username 新用户用户名
     * @param password 新用户密码
     * @param level 新用户权限等级
     */
    private suspend fun addUser(database: AsyncSqlHelper, username: String, password: String, vararg level: String) {
        val firstUser = UserData(username, "$username$password$username$password".sha256()!!, level.toList())
        database.insert(firstUser)
    }

    /**
     * 用于处理注册逻辑的函数
     * 第一个注册的用户不需要验证，并且固定是admin权限
     * 后续注册的用户需要有admin权限的token
     * 需要username，password以及level（可选，默认user）来注册一个新用户
     * @param content 请求的request对象
     * @return 处理结果，json数据
     */
    suspend fun register(
        database: AsyncSqlHelper,
        secretKey: Int,
        content: HttpContent,
        newServer: Boolean = false
    ): String {
        //如果数据库内无任何用户，则可以直接创建一个admin权限的用户
        return if (newServer) {
            //如果没有数据，说明现在没有任何用户注册
            //可以直接创建一个admin权限的用户
            //获取新用户用户名
            val username = content.getHeader("username")
                ?: content.getParam("username")
                ?: return "{\"state\":false,\"result\":\"user name is null\"}"
            //获取新用户密码
            val password = content.getHeader("password")
                ?: content.getParam("password")
                ?: return "{\"state\":false,\"result\":\"password is null\"}"
            //添加一个admin权限的新用户
            addUser(database, username, password, "admin")
            //返回成功信息
            "{\"state\":true,\"result\":\"${TokenData.getToken(
                username,
                password,
                secretKey = secretKey,
                database = database
            )}\"}"
        } else {
            //解析token
            val token = TokenData.parseToken(
                content.getParam("token") ?: return "{\"state\":false,\"result\":\"no token get\"}",
                secretKey
            )  //客户端没有发送token
                ?: return "{\"state\":false,\"result\":\"cant parse token\"}" //token无法解析
            when {
                //用户权限不是admin
                token.lev?.contains("admin") != true -> "{\"state\":false,\"result\":\"token user not admin\"}"
                //满足以上两个调解则注册新用户
                else -> {
                    //获取新用户用户名
                    val username =
                        content.getParam("username") ?: return "{\"state\":false,\"result\":\"user name is null\"}"
                    //获取新用户密码
                    val password =
                        content.getParam("password") ?: return "{\"state\":false,\"result\":\"password is null\"}"
                    //获取要注册的用户的权限
                    val level = content.getParam("level")
                    //添加新用户
                    addUser(database, username, password, level ?: "user")
                    //返回成功信息
                    "{\"state\":true,\"result\":\"${TokenData.getToken(
                        username, password,
                        secretKey = secretKey,
                        database = database
                    )}\"}"
                }
            }
        }
    }
}