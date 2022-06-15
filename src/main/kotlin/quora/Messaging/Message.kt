package quora.Messaging

data class Message(val success: Boolean, val message: String, val data: Map<String, String>? = null)