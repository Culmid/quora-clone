package quora.Services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import quora.DTOs.LoginDetailsDTO
import quora.Entities.User
import quora.Repositories.UserRepository

@Service
class UserService {

    @Autowired
    private var userRepository: UserRepository? = null

    fun registerUser(user: User): User? {
        return userRepository?.save(user)
    }

    fun isUserValid(loginDetails: LoginDetailsDTO): User? {
        val potentialUser = userRepository?.findByEmail(loginDetails.email)
        return if (potentialUser != null && BCryptPasswordEncoder().matches(loginDetails.password, potentialUser.password)) {
            potentialUser
        } else {
            null
        }
    }
}