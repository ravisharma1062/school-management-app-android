package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.Conversation
import com.school.app.domain.model.ConversationContact
import com.school.app.domain.model.ConversationCreateRequest
import com.school.app.domain.model.Message
import com.school.app.domain.model.MessageCreateRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun start(otherUserId: String): Outcome<Conversation> =
        safeApiCall { api.startConversation(ConversationCreateRequest(otherUserId)) }

    suspend fun list(): Outcome<List<Conversation>> = safeApiCall { api.conversations() }

    suspend fun contacts(): Outcome<List<ConversationContact>> = safeApiCall { api.conversationContacts() }

    suspend fun sendMessage(conversationId: String, body: String): Outcome<Message> =
        safeApiCall { api.sendMessage(conversationId, MessageCreateRequest(body)) }

    suspend fun messages(conversationId: String): Outcome<List<Message>> =
        safeApiCall { api.messages(conversationId) }
}
