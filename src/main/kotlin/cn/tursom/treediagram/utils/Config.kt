package cn.tursom.treediagram.utils

import cn.tursom.utils.xml.Constructor
import cn.tursom.utils.xml.ElementName

@Suppress("unused")
@ElementName("config")
data class Config(
    @Constructor("setPort") val port: Int = 12345,
    @Constructor("setLogPath") val logPath: String = "log",
    @Constructor("setLogFile") val logFile: String = "modLog",
    @Constructor("setMaxLogSize") val maxLogSize: Int = 64 * 1024,
    @Constructor("setLogFileCount") val logFileCount: Int = 24,
    @Constructor("setDatabase") val database: String = "TreeDiagram.db"
) {
    fun setPort(port: String) = port.toIntOrNull() ?: 12345
    fun setLogPath(logPath: String?) = logPath ?: "log"
    fun setLogFile(modLogFile: String?) = modLogFile ?: "modLog"
    fun setMaxLogSize(maxLogSize: String?) = maxLogSize?.toIntOrNull() ?: 64 * 1024
    fun setLogFileCount(logFileCount: String?) = logFileCount?.toIntOrNull() ?: 24
    fun setDatabase(database: String?) = database ?: "TreeDiagram.db"
}