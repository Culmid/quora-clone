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
import quora.DTOs.ProfileDTO
import quora.Messaging.Message
import quora.Entities.User
import quora.Messaging.ProfileMessage
import quora.Services.JwtService
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
    private val jwtService: JwtService? = null

    @GetMapping
    fun indexGet(): Message {
        return Message(true, "GET Message")
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun indexPost(): Message {
        return Message(true, "POST Message", mapOf("Data" to "Stuff"))
    }

    @GetMapping("/search/accounts")
    fun getProfile(@RequestHeader("authorization", required = false) auth: String?, @RequestParam(name = "email") email: String): ResponseEntity<Message> {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Authentication (Bearer) Token Missing from Header"))
        }

        if (jwtService?.parseJWT(auth) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Auth Token Invalid/Expired"))
        }

        val potentialUser = userService?.getUserByEmail(email)

        return if (potentialUser == null) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(Message(false, "Profile with Requested Email Not Found"))
        } else {
            ResponseEntity.status(HttpStatus.OK).body(Message(true, "Profile Details for $email", mapOf("id" to potentialUser.id.toString(), "firstName" to potentialUser.firstName, "lastName" to potentialUser.lastName)))
        }
    }

    @PostMapping("/accounts/follow")
    fun followUser(@RequestHeader("authorization", required = false) auth: String?, @RequestBody body: Map<String, String>): ResponseEntity<Message> {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Authentication (Bearer) Token Missing from Header"))
        }
        val jwt = jwtService?.parseJWT(auth)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Auth Token Invalid/Expired"))

        if (!body.containsKey("userId") || body["userId"] == "") {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "UserId Required"))
        }

        val userId = try {
            body["userId"]?.toInt() ?: -1
        } catch (e: java.lang.NumberFormatException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "UserId should be Integer Value"))
        }

        if (userService?.userIdExists(userId) != true) { // Guard for userService being null
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Message(false, "Profile with Requested userId Not Found"))
        }

        return if (userService.addFollowerRelationship(userId, jwt.body.id.toInt())) {
            ResponseEntity.status(HttpStatus.OK).body(Message(true, "Follow (${jwt.body.id} -> $userId) Successful"))
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "Invalid Follow Request"))
        }
    }


    @GetMapping("/accounts/following")
    fun getFollowing(@RequestHeader("authorization", required = false) auth: String?): ResponseEntity<ProfileMessage> {
        if (auth == null) { // Extract Auth
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ProfileMessage(false, "Authentication (Bearer) Token Missing from Header"))
        }
        val jwt = jwtService?.parseJWT(auth)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ProfileMessage(false, "Auth Token Invalid/Expired"))

        return ResponseEntity.status(HttpStatus.OK).body(ProfileMessage(true, "Successfully Retrieved Followed Accounts", mapOf("following" to (userService?.getFollowingList(jwt.body.id.toInt()) ?: emptyList()))))
    }

    @GetMapping("/accounts/followers")
    fun getFollowers(@RequestHeader("authorization", required = false) auth: String?): ResponseEntity<ProfileMessage> {
        if (auth == null) { // Extract Auth
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ProfileMessage(false, "Authentication (Bearer) Token Missing from Header"))
        }
        val jwt = jwtService?.parseJWT(auth)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ProfileMessage(false, "Auth Token Invalid/Expired"))

        return ResponseEntity.status(HttpStatus.OK).body(ProfileMessage(true, "Successfully Retrieved Following Accounts", mapOf("following" to (userService?.getFollowerList(jwt.body.id.toInt()) ?: emptyList()))))
    }
}