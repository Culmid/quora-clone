package quora

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

    @NotBlank
    var firstName: String = ""

    @NotBlank
    var lastName: String = ""

    @NotBlank
    @Column(unique=true)
    @Email(regexp = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$")
    var email: String = ""

    @NotBlank
    @Size(min = 8)
    @Column(length = 60)
    var password: String = ""
        set(value) {
            field = BCryptPasswordEncoder().encode(value)
        }
}
