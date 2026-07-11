package com.school.app.ui.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.school.app.R
import com.school.app.domain.model.Conversation
import com.school.app.domain.model.ConversationContact
import com.school.app.domain.model.Message
import com.school.app.domain.model.Role
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.stringRes
import com.school.app.viewmodel.ConversationThreadViewModel
import com.school.app.viewmodel.MessagesViewModel

@Composable
fun MessagesListScreen(
    role: Role,
    onBack: () -> Unit,
    onOpenConversation: (Conversation) -> Unit,
    viewModel: MessagesViewModel = hiltViewModel(),
) {
    val state = viewModel.state

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val started = viewModel.startedConversationId
    if (started != null) {
        val conversation = state.conversations.find { it.id == started }
        if (conversation != null) {
            onOpenConversation(conversation)
            viewModel.consumeStartedConversation()
        }
    }

    Scaffold(topBar = { AppTopBar(stringRes(R.string.messages_title), onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                state.loading && state.conversations.isEmpty() && state.contacts.isEmpty() -> CenteredLoading()
                state.error != null && state.conversations.isEmpty() -> ErrorState(state.error, onRetry = viewModel::refresh)
                state.conversations.isEmpty() && state.contacts.isEmpty() ->
                    EmptyState(
                        stringRes(if (role == Role.PARENT) R.string.messages_none_parent else R.string.messages_none_teacher),
                    )
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.conversations, key = { "c-${it.id}" }) { c ->
                        ConversationRow(otherName(c, role), onClick = { onOpenConversation(c) })
                    }
                    val existingContactIds = state.conversations.map { if (role == Role.TEACHER) it.parentId else it.teacherId }.toSet()
                    val newContacts = state.contacts.filter { it.id !in existingContactIds }
                    if (newContacts.isNotEmpty()) {
                        item {
                            Text(
                                stringRes(R.string.messages_start_new),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(newContacts, key = { "n-${it.id}" }) { contact ->
                            ContactRow(contact, enabled = !viewModel.starting, onClick = { viewModel.startConversation(contact.id) })
                        }
                    }
                }
            }
        }
    }
}

private fun otherName(c: Conversation, role: Role) = if (role == Role.TEACHER) c.parentName else c.teacherName

@Composable
private fun ConversationRow(name: String, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ContactRow(contact: ConversationContact, enabled: Boolean, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(contact.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ConversationThreadScreen(
    title: String,
    currentUserId: String?,
    onBack: () -> Unit,
    viewModel: ConversationThreadViewModel = hiltViewModel(),
) {
    val state = viewModel.state

    Scaffold(topBar = { AppTopBar(title, onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(Modifier.weight(1f)) {
                when {
                    state.loading && state.messages.isEmpty() -> CenteredLoading()
                    state.error != null && state.messages.isEmpty() -> ErrorState(state.error, onRetry = viewModel::load)
                    state.messages.isEmpty() -> EmptyState(stringRes(R.string.messages_none_yet))
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.messages, key = { it.id }) { MessageBubble(it, mine = it.senderId == currentUserId) }
                    }
                }
            }
            HorizontalDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = viewModel.draft,
                    onValueChange = { viewModel.draft = it },
                    placeholder = { Text(stringRes(R.string.messages_type_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::send, enabled = !viewModel.sending && viewModel.draft.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringRes(R.string.messages_send))
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, mine: Boolean) {
    val bubbleColor = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            message.body,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
