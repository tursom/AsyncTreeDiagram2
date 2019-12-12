package cn.tursom.treediagram

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

@Configuration
@Import(JdbcConfig::class)
@ComponentScan("cn.tursom.treediagram")
@PropertySource("classpath:jdbcConfig.properties")
open class SpringConfig {
}

@Configuration
open class JdbcConfig {
    @Value("\${jdbc.url}")
    private val url = ""
}