package quora.Messaging

import quora.DTOs.QuestionOutputDTO

data class QuestionsMessage(val success: Boolean, val message: String, val data: Map<String, List<QuestionOutputDTO>>? = null)