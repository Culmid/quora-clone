package quora.Repositories

import org.springframework.data.repository.CrudRepository
import quora.Entities.User

interface UserRepository: CrudRepository<User, Int> {
    fun findByEmail(email: String): User?
}