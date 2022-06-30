package quora.Controllers

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.PropertySource
import org.springframework.context.support.DefaultMessageSourceResolvable
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import quora.DTOs.LoginDetailsDTO
import quora.DTOs.PasswordChangeDTO
import quora.DTOs.PasswordResetDTO
import quora.Entities.User
import quora.Messaging.Message
import quora.Services.JwtService
import quora.Services.UserService
import javax.validation.Valid

@RestController
@RequestMapping("/auth")
@PropertySource("classpath:application.properties")
class AuthController {
    @Autowired
    private val userService: UserService? = null

    @Autowired
    private val jwtService: JwtService? = null

    @Autowired
    private val env: Environment? = null

    @PostMapping("/register")
    fun registerUser(@Valid @RequestBody user: User, bindingResult: BindingResult): ResponseEntity<Message> {
        val emailExists = userService?.emailExists(user.email) ?: false
        if (bindingResult.hasErrors() || emailExists) {
            val errorMap = mutableMapOf<String, String>()
            for (error in bindingResult.allErrors) {
                errorMap[(error.arguments?.get(0) as DefaultMessageSourceResolvable).code.toString()] = error.defaultMessage.toString()
            }
            if (emailExists) {
                errorMap["email"] = "Already in Use"
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "Error(s) Found in User Registration", errorMap.toSortedMap()));
        }

        val response = userService?.registerUser(user)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Message(true, "User Registered", mapOf("Jwt-Token" to (jwtService?.generateJWT(response?.id ?: -1) ?: "123"))))
    }

    @PostMapping("/login")
    fun loginUser(@Valid @RequestBody loginDetails: LoginDetailsDTO, bindingResult: BindingResult): ResponseEntity<Message> {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "Email and Password Fields Required"))
        }

        val user = userService?.isUserValid(loginDetails)
        return if (user == null) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "Email/Password Invalid"))
        } else {
            ResponseEntity
                .status(HttpStatus.OK)
                .body(Message(true, "User Logged In", mapOf("Jwt-Token" to (jwtService?.generateJWT(user.id) ?: "123"))))
        }
    }

    @PostMapping("/password")
    fun passwordChange(@RequestHeader("authorization", required = false) auth: String?, @Valid @RequestBody passwordRequest: PasswordChangeDTO, bindingResult: BindingResult): ResponseEntity<Message> {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Authentication (Bearer) Token Missing from Header"))
        }

        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "CurrentPassword and NewPassword Fields Required"))
        }

        val jws: Jws<Claims>? = jwtService?.parseJWT(auth)

        return if (jws == null) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Auth Token Invalid/Expired"))
        } else {
            val response = userService?.updatePassword(jws.body.id.toInt(), passwordRequest) ?: false

            if (response) {
                ResponseEntity.status(HttpStatus.OK).body(Message(true, "Password Updated"))
            } else {
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Message(false, "Provided User Credentials/Requested Password - Invalid"))
            }
        }
    }

    @PostMapping("/password-reset")
    fun forgotPassword(@RequestBody body: Map<String, String>): ResponseEntity<Message> {
        if (!body.containsKey("email") || body["email"] == "") {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "Email Required"))
        }

        val toEmail = body["email"] ?: ""
        userService?.constructAndSendPasswordRecovery(env?.get("spring.mail.username") ?: "example@gmail.com", toEmail)

        return ResponseEntity.status(HttpStatus.OK).body(Message(true, "5 Digit Recovery Key will be sent to: $toEmail"))
    }

    @PostMapping("/password-reset/confirm")
    fun forgotPasswordConfirm(@Valid @RequestBody passwordResetDetails: PasswordResetDTO, bindingResult: BindingResult): ResponseEntity<Message> {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "Invalid Password-Reset Confirmation Request"))
        }

        if (passwordResetDetails.newPassword.length < 8) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "NewPassword Invalid"))
        }

        return if (userService?.updateResetPassword(passwordResetDetails) == true) {
            ResponseEntity.status(HttpStatus.OK).body(Message(true, "Password Successfully Reset"))
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Email and recoveryKey Pair Invalid"))
        }
    }
}