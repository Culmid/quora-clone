package quora.Entities

import org.springframework.data.redis.core.RedisHash
import java.io.Serializable


@RedisHash("RedisEntity", timeToLive = 600L)
class RedisEntity(var id: String? = null, var value: String? = null): Serializable