package quora.Controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.PropertySource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import quora.Messaging.Message
import quora.Messaging.ProfileMessage
import quora.Services.JwtService
import quora.Services.UserService

@RestController
@RequestMapping("/accounts")
@PropertySource("classpath:application.properties")
class AccountsController {

    @Autowired
    private val userService: UserService? = null

    @Autowired
    private val jwtService: JwtService? = null

    @PostMapping("/follow")
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

    @GetMapping("/following")
    fun getFollowing(@RequestHeader("authorization", required = false) auth: String?): ResponseEntity<ProfileMessage> {
        if (auth == null) { // Extract Auth
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ProfileMessage(false, "Authentication (Bearer) Token Missing from Header"))
        }
        val jwt = jwtService?.parseJWT(auth)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ProfileMessage(false, "Auth Token Invalid/Expired"))

        return ResponseEntity.status(HttpStatus.OK).body(ProfileMessage(true, "Successfully Retrieved Followed Accounts", mapOf("following" to (userService?.getFollowingList(jwt.body.id.toInt()) ?: emptyList()))))
    }

    @GetMapping("/followers")
    fun getFollowers(@RequestHeader("authorization", required = false) auth: String?): ResponseEntity<ProfileMessage> {
        if (auth == null) { // Extract Auth
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ProfileMessage(false, "Authentication (Bearer) Token Missing from Header"))
        }
        val jwt = jwtService?.parseJWT(auth)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ProfileMessage(false, "Auth Token Invalid/Expired"))

        return ResponseEntity.status(HttpStatus.OK).body(ProfileMessage(true, "Successfully Retrieved Following Accounts", mapOf("following" to (userService?.getFollowerList(jwt.body.id.toInt()) ?: emptyList()))))
    }
}