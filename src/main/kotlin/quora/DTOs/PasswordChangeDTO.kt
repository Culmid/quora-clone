package quora.DTOs

import javax.validation.constraints.NotBlank

data class PasswordChangeDTO(@field:NotBlank val currentPassword: String ="", @field:NotBlank val newPassword: String ="")