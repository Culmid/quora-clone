package quora.DTOs

import javax.validation.constraints.NotBlank

data class LoginDetailsDTO(@field:NotBlank val email: String = "", @field:NotBlank val password: String = "")