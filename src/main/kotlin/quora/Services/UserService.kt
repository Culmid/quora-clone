package quora.Services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import quora.DTOs.LoginDetailsDTO
import quora.DTOs.PasswordChangeDTO
import quora.Entities.User
import quora.Repositories.UserRepository
import java.util.concurrent.ThreadLocalRandom

@Service
class UserService {

    @Autowired
    private var userRepository: UserRepository? = null

    @Autowired
    private var mailSender: JavaMailSender? = null

    fun emailExists(email: String): Boolean {
        return userRepository?.findByEmail(email) != null
    }

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

    fun updatePassword(userId: Int, passwordRequest: PasswordChangeDTO): Boolean {
        val potentialUser = userRepository?.findById(userId)?.get()

        return if (potentialUser != null && BCryptPasswordEncoder().matches(passwordRequest.currentPassword, potentialUser.password)) {
                    try {
                        potentialUser.password = passwordRequest.newPassword
                        userRepository?.save(potentialUser)
                        true
                    } catch (e: Exception) { // Bit of a Foot Gun :)
                        false
                    }
                } else {
                    false
                }
    }

    fun constructAndSendPasswordRecovery(fromEmail: String, toEmail: String) {
        val randomNum: Int = ThreadLocalRandom.current().nextInt(10000, 99999 + 1)

        val message = SimpleMailMessage()
        message.setFrom(fromEmail)
        message.setTo(toEmail)
        message.setText("Reset Code: $randomNum")
        message.setSubject("Quora Clone - Password Reset")

        mailSender?.send(message)
    }
}