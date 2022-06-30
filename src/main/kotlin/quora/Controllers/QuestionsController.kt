package quora.Controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.PropertySource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import quora.DTOs.QuestionDTO
import quora.Messaging.Message
import quora.Services.JwtService
import quora.Services.UserService
import javax.validation.Valid

@RestController
@RequestMapping("/questions")
@PropertySource("classpath:application.properties")
class QuestionsController {
    @Autowired
    private val userService: UserService? = null

    @Autowired
    private val jwtService: JwtService? = null

    @PostMapping
    fun postQuestion(@RequestHeader("authorization", required = false) auth: String?, @Valid @RequestBody question: QuestionDTO, bindingResult: BindingResult): ResponseEntity<Message>{
        // Handle Auth
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Authentication (Bearer) Token Missing from Header"))
        }
        val jwt = jwtService?.parseJWT(auth)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message(false, "Auth Token Invalid/Expired"))

        // Handle Validation Err
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "Title and Description Fields Required"))
        }

        return  if (userService?.postQuestion(jwt.body.id.toInt(), question) == true) {
            ResponseEntity.status(HttpStatus.OK).body(Message(true, "Question Posted Successfully: ${question.title}"))
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Message(false, "Unsuccessful in Adding Question"))
        }
    }

//    @GetMapping
//    fun getQuestions(): ResponseEntity<Message> {
//
//    }
}