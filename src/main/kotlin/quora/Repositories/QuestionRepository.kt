package quora.Repositories

import org.springframework.data.repository.CrudRepository
import quora.Entities.Question

// Currently Placeholder
interface QuestionRepository: CrudRepository<Question, Int>