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

    @NotBlank(message = "Missing - Field Required")
    var firstName: String = ""

    @NotBlank(message = "Missing - Field Required")
    var lastName: String = ""

    @NotBlank(message = "Missing - Field Required")
    @Column(unique=true)
    @Email(regexp = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$", message = "Format Invalid - Please enter valid email")
    var email: String = ""

    @NotBlank(message = "Missing - Field Required")
    @Size(min = 8, message = "Invalid - Length must be > 8")
    @Column(length = 60)
    var password: String = ""

    @PrePersist
    @PreUpdate
    fun prePersist() {
        password = BCryptPasswordEncoder().encode(password)
    }

    @OneToMany(
        mappedBy = "author",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    private val questions: MutableSet<Question> = mutableSetOf()

    fun addQuestion(question: Question) {
        questions.add(question)
        question.author = this
    }

    fun getQuestions(): Set<Question> {
        return questions
    }
}
