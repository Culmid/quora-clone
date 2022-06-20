package quora.Config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import quora.Entities.RedisEntity


@Configuration
@PropertySource("classpath:application.properties")
class RedisConfig {
    @Autowired
    private val env: Environment? = null

    @Bean
    fun jedisConnectionFactory(): JedisConnectionFactory {
        val config = RedisStandaloneConfiguration(
            env?.getProperty("spring.redis.host") ?: "localhost",
            env?.getProperty("spring.redis.port")?.toInt() ?: 6379)

        config.setPassword(env?.getProperty("spring.redis.password") ?: "123")

        return JedisConnectionFactory(config)
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, RedisEntity> {
        val template: RedisTemplate<String, RedisEntity> = RedisTemplate<String, RedisEntity>()
        template.setConnectionFactory(jedisConnectionFactory())
        return template
    }
}