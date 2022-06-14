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
    fun passwordChange(@RequestHeader("authorization") auth: String): String {
        val bearerToken = auth.split(" ")[1] // Assume Correct Format -> Bearer <Token>

        var jws: Jws<Claims>;
        val secretKey = env?.getProperty("secret-key") ?: "12345789"
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

        try {
            jws = Jwts.parserBuilder()  // (1)
                      .setSigningKey(key)         // (2)
                      .build()                    // (3)
                      .parseClaimsJws(bearerToken); // (4)

            println(jws.body.subject)
            println(jws.body.id)
            // we can safely trust the JWT

        } catch (e: JwtException) {       // (5)
                return "fook"
                // we *cannot* use the JWT as intended by its creator
        }

        return auth
    }

    private fun generateJWT(id: Int): String {
        val secretKey = env?.getProperty("secret-key") ?: "12345789"
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

        return Jwts.builder()
                   .setIssuer("quora")
                   .setSubject("userAuth")
                   .setId("$id")
                   .setIssuedAt(Date.from(Instant.now())) // Current Time
                   .setExpiration(Date.from(Instant.now().plusSeconds(86400L))) // One Day Later
                   .signWith(key)
                   .compact()
    }
}