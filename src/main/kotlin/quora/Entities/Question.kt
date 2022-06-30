package quora.Entities

import javax.persistence.*

@Entity
@Table(name="questions")
class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0

    var title: String = ""

    var description: String = ""

    @ManyToOne
    @JoinColumn(name = "author_id")
    var author: User? = null
}