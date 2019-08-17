package cn.tursom.treediagram.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object Json {
    var prettyGson: Gson = GsonBuilder().setPrettyPrinting().create()
    var gson: Gson = Gson()
}