package quora.DTOs

import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

data class PasswordResetDTO(@field:NotBlank val email: String ="", @field:NotBlank val newPassword: String ="", @field:Min(10000) @field:Max(99999) val recoveryKey: Int = 0)