package quora.Messaging

data class Message (val success: Boolean, val message: String, val data: String? = null)