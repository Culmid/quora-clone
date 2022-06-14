package quora.Controllers

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import quora.DTOs.LoginDetailsDTO
import quora.DTOs.PasswordChangeDTO
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
        val response = userService?.registerUser(user)
        return ResponseEntity.created(URI("URI_PLACEHOLDER"))
                             .header("Jwt-Token", generateJWT(response?.id ?: -1))
                             .body(response)
    }

    @PostMapping("/auth/login")
    fun loginUser(@RequestBody loginDetails: LoginDetailsDTO): ResponseEntity<Message> {
        val user = userService?.isUserValid(loginDetails)

        if (user != null) {
            return ResponseEntity
                   .ok()
                   .header("Jwt-Token", generateJWT(user.id))
                   .body(Message(true, "User Logged In"))
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Message(false, "Login Unsuccessful"))
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