package cn.tursom.treediagram.environment

import java.util.logging.Logger

interface Environment : ModEnvironment, ServiceEnvironment, ServerEnvironment {
    val logger: Logger
}

