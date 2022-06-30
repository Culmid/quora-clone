package quora.Messaging

import quora.DTOs.ProfileDTO

data class ProfileMessage (val success: Boolean, val message: String, val data: Map<String, List<ProfileDTO>>? = null)