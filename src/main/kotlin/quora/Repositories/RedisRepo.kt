package quora.Repositories

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import quora.Entities.RedisEntity


@Repository
interface RedisRepo: CrudRepository<RedisEntity, String>