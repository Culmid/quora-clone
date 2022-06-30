package quora.Services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import quora.DTOs.*
import quora.Entities.FollowRelationship
import quora.Entities.Question
import quora.Entities.User
import quora.Entities.RedisEntity
import quora.Repositories.FollowRepository
import quora.Repositories.RedisRepo
import quora.Repositories.UserRepository
import java.util.concurrent.ThreadLocalRandom

@Service
class UserService {

    @Autowired
    private var userRepository: UserRepository? = null

    @Autowired
    private var followRepository: FollowRepository? = null

    @Autowired
    private var mailSender: JavaMailSender? = null

    @Autowired
    private var redisRepo: RedisRepo? = null

    fun userIdExists(id: Int): Boolean {
        return userRepository?.findById(id)?.isEmpty == false
    }

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
        val potentialUser = userRepository?.findByEmail(toEmail)

        if (potentialUser != null) {
            val randomNum: Int = ThreadLocalRandom.current().nextInt(10000, 99999 + 1)

            val message = SimpleMailMessage()
            message.setFrom(fromEmail)
            message.setTo(toEmail)
            message.setText("Reset Code: $randomNum")
            message.setSubject("Quora Clone - Password Reset")

            try {
                mailSender?.send(message)
                redisRepo?.save(RedisEntity("password-reset-token-${potentialUser.id}", randomNum.toString()))
            } catch (e: MailException) {
                System.err.println(e)
            }
        }
    }

    fun updateResetPassword(passwordResetDetails: PasswordResetDTO): Boolean {
        val potentialUser = userRepository?.findByEmail(passwordResetDetails.email)
        if (potentialUser != null) {
            val redisEntity = redisRepo?.findById("password-reset-token-${potentialUser.id}") // Fix .get()

            if (redisEntity?.isPresent == true && redisEntity.get().value?.toInt() == passwordResetDetails.recoveryKey) {
                // Update Password
                potentialUser.password = passwordResetDetails.newPassword
                userRepository?.save(potentialUser)
                return true
            }
        }

        return false
    }

    fun getUserByEmail(email: String): User? {
        return userRepository?.findByEmail(email)
    }

    fun addFollowerRelationship(followedUserId: Int, followerId: Int): Boolean {
        println("followedUserId: $followedUserId")
        println("followerId: $followerId")

        val followRelationship = FollowRelationship()

        if (followedUserId != followerId) {
            val followedUser = userRepository?.findById(followedUserId)?.get()
            val follower = userRepository?.findById(followerId)?.get()

            followRelationship.followedUser = followedUser
            followRelationship.follower = follower

            // Persist Changes
            val existingRelationship = followRepository?.findByFollowedUserAndFollower(followedUser, follower)
            if (existingRelationship == null) {
                followRepository?.save(followRelationship)
                return true
            }
        }

        return false
    }

    fun getFollowingList(userId: Int): List<ProfileDTO> {
        val follower = userRepository?.findById(userId)?.get()

        return followRepository?.findAllByFollower(follower)?.map {// Questionable null checks ;)
            val followedUser = it.followedUser
            ProfileDTO(followedUser?.id ?: -1, followedUser?.firstName ?: "", followedUser?.lastName ?: "")
        } ?: emptyList()
    }

    fun getFollowerList(userId: Int): List<ProfileDTO> {
        val followedUser = userRepository?.findById(userId)?.get()

        return followRepository?.findAllByFollowedUser(followedUser)?.map {// Questionable null checks ;)
            val follower = it.follower
            ProfileDTO(follower?.id ?: -1, follower?.firstName ?: "", follower?.lastName ?: "")
        } ?: emptyList()
    }

    fun postQuestion(userId: Int, questionDTO: QuestionDTO): Boolean {
        val user = userRepository?.findById(userId)?.get() ?: return false

        val question = Question()
        question.title = questionDTO.title
        question.description = questionDTO.description
        user.addQuestion(question)

        userRepository?.save(user)

        return true
    }
}