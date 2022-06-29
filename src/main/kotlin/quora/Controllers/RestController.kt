package quora.Controllers

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
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
import quora.Messaging.Message
import quora.Entities.User
import quora.Services.UserService
import java.time.Instant
import java.util.*
import javax.validation.Valid


@RestController
@PropertySource("classpath:application.properties")
class RestController {
    @Autowired
    private val userService: UserService? = null

    @Autowired
    private val env: Environment? = null

    @GetMapping
    fun indexGet(): Message {
        return Message(true, "GET Message")
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun indexPost(): Message {
        return Message(true, "POST Message", mapOf("Data" to "Stuff"))
    }

    @PostMapping("/auth/register")
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
            .body(Message(true, "User Registered", mapOf("Jwt-Token" to generateJWT(response?.id ?: -1))))
    }

    @PostMapping("/auth/login")
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
            .body(Message(true, "User Logged In", mapOf("Jwt-Token" to generateJWT(user.id))))
        }
    }

    @PostMapping("/auth/password")
    fun passwordChange(@RequestHeader("authorization", required = false) auth: String?, @Valid @RequestBody passwordRequest: PasswordChangeDTO, bindingResult: BindingResult): ResponseEntity<Message> {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Authentication (Bearer) Token Missing from Header"))
        }

        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "CurrentPassword and NewPassword Fields Required"))
        }

        val jws: Jws<Claims>? = parseJWT(auth)

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

    @PostMapping("/auth/password-reset")
    fun forgotPassword(@RequestBody body: Map<String, String>): ResponseEntity<Message> {
        if (!body.containsKey("email") || body["email"] == "") {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "Email Required"))
        }

        val toEmail = body["email"] ?: ""
        userService?.constructAndSendPasswordRecovery(env?.get("spring.mail.username") ?: "example@gmail.com", toEmail)

        return ResponseEntity.status(HttpStatus.OK).body(Message(true, "5 Digit Recovery Key will be sent to: $toEmail"))
    }


    @PostMapping("/auth/password-reset/confirm")
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

    @GetMapping("/search/accounts")
    fun getProfile(@RequestHeader("authorization", required = false) auth: String?, @RequestParam(name = "email") email: String): ResponseEntity<Message> {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Authentication (Bearer) Token Missing from Header"))
        }

        if (parseJWT(auth) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Auth Token Invalid/Expired"))
        }

        val potentialUser = userService?.getUserByEmail(email)

        return if (potentialUser == null) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(Message(false, "Profile with Requested Email Not Found"))
        } else {
            ResponseEntity.status(HttpStatus.OK).body(Message(true, "Profile Details for $email", mapOf("id" to potentialUser.id.toString(), "firstName" to potentialUser.firstName, "lastName" to potentialUser.lastName)))
        }
    }

    private fun generateJWT(id: Int): String {
        val secretKey = env?.getProperty("jwt-secret-key") ?: "12345789"
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

        return Jwts.builder()
                   .setIssuer("quora")
                   .setSubject("userAuth")
                   .setId("$id")
                   .setIssuedAt(Date.from(Instant.now())) // Current Time
                   .setExpiration(Date.from(Instant.now().plusSeconds(env?.get("jwt-expire-period")?.toLong() ?: 86400L))) // One Day Later
                   .signWith(key)
                   .compact()
    }

    private fun parseJWT(authHeader: String): Jws<Claims>? {
        val bearerToken = authHeader.split(" ")[1] // Assume Correct Format -> Bearer <Token>
        val secretKey = env?.getProperty("jwt-secret-key") ?: "12345789"
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

        return  try {
                    Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(bearerToken);
                } catch (e: JwtException) {
                    null
                }
    }
}