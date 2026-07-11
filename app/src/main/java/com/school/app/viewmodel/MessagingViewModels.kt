package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.repository.ConversationRepository
import com.school.app.domain.model.Conversation
import com.school.app.domain.model.ConversationContact
import com.school.app.domain.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    data class State(
        val conversations: List<Conversation> = emptyList(),
        val contacts: List<ConversationContact> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
    )

    var state by mutableStateOf(State())
        private set

    var starting by mutableStateOf(false)
        private set

    var startedConversationId by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            val conversations = conversationRepository.list()
            val contacts = conversationRepository.contacts()
            state = when {
                conversations is Outcome.Failure -> state.copy(loading = false, error = conversations.message)
                contacts is Outcome.Failure -> state.copy(loading = false, error = contacts.message)
                conversations is Outcome.Success && contacts is Outcome.Success -> state.copy(
                    loading = false,
                    conversations = conversations.data,
                    contacts = contacts.data,
                    error = null,
                )
                else -> state.copy(loading = false)
            }
        }
    }

    fun startConversation(otherUserId: String) {
        if (starting) return
        starting = true
        viewModelScope.launch {
            when (val result = conversationRepository.start(otherUserId)) {
                is Outcome.Success -> {
                    startedConversationId = result.data.id
                    refresh()
                }
                is Outcome.Failure -> state = state.copy(error = result.message)
            }
            starting = false
        }
    }

    fun consumeStartedConversation() {
        startedConversationId = null
    }
}

@HiltViewModel
class ConversationThreadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    data class State(
        val messages: List<Message> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
    )

    var state by mutableStateOf(State())
        private set

    var draft by mutableStateOf("")

    var sending by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            state = when (val result = conversationRepository.messages(conversationId)) {
                is Outcome.Success -> state.copy(loading = false, messages = result.data, error = null)
                is Outcome.Failure -> state.copy(loading = false, error = result.message)
            }
        }
    }

    fun send() {
        val body = draft.trim()
        if (body.isEmpty() || sending) return
        sending = true
        viewModelScope.launch {
            when (val result = conversationRepository.sendMessage(conversationId, body)) {
                is Outcome.Success -> {
                    draft = ""
                    load()
                }
                is Outcome.Failure -> state = state.copy(error = result.message)
            }
            sending = false
        }
    }
}
