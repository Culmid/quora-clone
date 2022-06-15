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

    @RequestMapping("/auth/password")
    fun passwordChange(@RequestHeader("authorization") auth: String, @RequestBody passwordRequest: PasswordChangeDTO): ResponseEntity<Message> {
        val bearerToken = auth.split(" ")[1] // Assume Correct Format -> Bearer <Token>
        val secretKey = env?.getProperty("secret-key") ?: "12345789"
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

        return try {
            val jws: Jws<Claims> = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(bearerToken);
            var response = userService?.updatePassword(jws.body.id.toInt(), passwordRequest) ?: false

            if (response) {
                ResponseEntity.ok().body(Message(true, "Password Updated"))
            } else {
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "Provided User Credentials Incorrect"))
            }
        } catch (e: JwtException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Auth Token Invalid"))
        }
    }

    private fun generateJWT(id: Int): String {
        val secretKey = env?.getProperty("secret-key") ?: "12345789"
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

        println(Date.from(Instant.now().plusSeconds(env?.get("expire-period")?.toLong() ?: 86400L)))
        return Jwts.builder()
                   .setIssuer("quora")
                   .setSubject("userAuth")
                   .setId("$id")
                   .setIssuedAt(Date.from(Instant.now())) // Current Time
                   .setExpiration(Date.from(Instant.now().plusSeconds(env?.get("expire-period")?.toLong() ?: 86400L))) // One Day Later
                   .signWith(key)
                   .compact()
    }
}