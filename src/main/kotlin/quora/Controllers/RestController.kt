package quora.Controllers

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import quora.DTOs.LoginDetailsDTO
import quora.Messaging.Message
import quora.Entities.User
import quora.Services.UserService
import java.net.URI
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
        return Message(true, "POST Message", "Data")
    }

    @PostMapping("/auth/register")
    fun registerUser(@Valid @RequestBody user: User): ResponseEntity<User?> {
        val secretKey = env?.getProperty("secret-key") ?: "12345789"
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))
        val response = userService?.registerUser(user)

        val jws = Jwts.builder()
            .setIssuer("quora")
            .setSubject("${response?.id}")
            .claim("firstName", response?.firstName ?: "No First Name")
            .claim("lastName", response?.lastName ?: "No Last Name")
            .claim("email", response?.email ?: "")
            .setIssuedAt(Date.from(Instant.now())) // Current Time
            .setExpiration(Date.from(Instant.now().plusSeconds(86400L))) // One Day Later
            .signWith(key)
            .compact()

        return ResponseEntity.created(URI("URI_PLACEHOLDER"))
                             .header("JWT-TOKEN", jws)
                             .body(response)
    }

    @PostMapping("/auth/login")
    fun loginUser(@RequestBody loginDetails: LoginDetailsDTO): ResponseEntity<Message> {
        val user = userService?.isUserValid(loginDetails)

        if (user != null) {
            val secretKey = env?.getProperty("secret-key") ?: "12345789"
            val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))
            val jws = Jwts.builder()
                .setIssuer("quora")
                .setSubject("${user.id}")
                .claim("firstName", user.firstName ?: "No First Name")
                .claim("lastName", user.lastName ?: "No Last Name")
                .claim("email", user.email ?: "")
                .setIssuedAt(Date.from(Instant.now())) // Current Time
                .setExpiration(Date.from(Instant.now().plusSeconds(86400L))) // One Day Later
                .signWith(key)
                .compact()

            return ResponseEntity
                .ok()
                .header("JWT-TOKEN", jws)
                .body(Message(true, "User Logged In"))
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Message(false, "Login Unsuccessful"))
    }
}