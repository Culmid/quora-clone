package quora.DTOs

import javax.validation.constraints.NotBlank

data class QuestionDTO(@field:NotBlank val title: String = "", @field:NotBlank val description: String = "")