package quora

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@Validated
class RestController {
    @Autowired
    private var userRepository: UserRepository? = null

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
    @ResponseStatus(HttpStatus.CREATED)
    fun registerUser(@Valid @RequestBody user: User): User? {
        return userRepository?.save(user)
    }
}