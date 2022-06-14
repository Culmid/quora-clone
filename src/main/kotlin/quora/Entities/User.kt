package quora.Entities

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import javax.persistence.*
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size


@Entity
@Table(name="users")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0

    @NotBlank(message = "firstName Cannot be Blank")
    var firstName: String = ""

    @NotBlank(message = "lastName Cannot be Blank")
    var lastName: String = ""

    @NotBlank(message = "email Cannot be Blank")
    @Column(unique=true)
    @Email(regexp = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$")
    var email: String = ""

    @NotBlank(message = "password Cannot be Blank")
    @Size(min = 8)
    @Column(length = 60)
    var password: String = ""

    @PrePersist
    fun prePersist() {
        password = BCryptPasswordEncoder().encode(password)
    }
}
