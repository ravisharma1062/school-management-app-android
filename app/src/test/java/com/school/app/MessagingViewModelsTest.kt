package com.school.app

import androidx.lifecycle.SavedStateHandle
import com.school.app.data.Outcome
import com.school.app.data.repository.ConversationRepository
import com.school.app.domain.model.Conversation
import com.school.app.domain.model.ConversationContact
import com.school.app.domain.model.Message
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.ConversationThreadViewModel
import com.school.app.viewmodel.MessagesViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MessagingViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ConversationRepository>()

    private fun conversation(id: String) =
        Conversation(id, "parent1", "Parent", "teacher1", "Teacher", "2026-07-16T00:00:00Z")

    // --- MessagesViewModel ---

    @Test
    fun `refresh only populates state once both conversations and contacts succeed`() = runTest {
        coEvery { repository.list() } returns Outcome.Success(listOf(conversation("c1")))
        coEvery { repository.contacts() } returns Outcome.Success(listOf(ConversationContact("u1", "Asha", "a@x.test")))

        val vm = MessagesViewModel(repository)

        assertEquals(listOf("c1"), vm.state.conversations.map { it.id })
        assertEquals(1, vm.state.contacts.size)
        assertFalse(vm.state.loading)
    }

    @Test
    fun `a failed conversations fetch surfaces its error even if contacts succeeded`() = runTest {
        coEvery { repository.list() } returns Outcome.Failure("conversations down")
        coEvery { repository.contacts() } returns Outcome.Success(emptyList())

        val vm = MessagesViewModel(repository)

        assertEquals("conversations down", vm.state.error)
    }

    @Test
    fun `a failed contacts fetch surfaces its error even if conversations succeeded`() = runTest {
        coEvery { repository.list() } returns Outcome.Success(listOf(conversation("c1")))
        coEvery { repository.contacts() } returns Outcome.Failure("contacts down")

        val vm = MessagesViewModel(repository)

        assertEquals("contacts down", vm.state.error)
    }

    @Test
    fun `starting a conversation guards against a second concurrent call`() = runTest {
        coEvery { repository.list() } returns Outcome.Success(emptyList())
        coEvery { repository.contacts() } returns Outcome.Success(emptyList())
        val vm = MessagesViewModel(repository)
        val gate = CompletableDeferred<Outcome<Conversation>>()
        coEvery { repository.start("u1") } coAnswers { gate.await() }

        vm.startConversation("u1")
        assertTrue(vm.starting)
        vm.startConversation("u1")

        gate.complete(Outcome.Success(conversation("c9")))

        coVerify(exactly = 1) { repository.start(any()) }
        assertEquals("c9", vm.startedConversationId)
    }

    @Test
    fun `consumeStartedConversation clears the id`() = runTest {
        coEvery { repository.list() } returns Outcome.Success(emptyList())
        coEvery { repository.contacts() } returns Outcome.Success(emptyList())
        coEvery { repository.start("u1") } returns Outcome.Success(conversation("c9"))
        val vm = MessagesViewModel(repository)

        vm.startConversation("u1")
        vm.consumeStartedConversation()

        assertEquals(null, vm.startedConversationId)
    }

    // --- ConversationThreadViewModel ---

    @Test
    fun `conversation thread requires a conversationId in the saved state`() {
        assertThrows(IllegalStateException::class.java) {
            ConversationThreadViewModel(SavedStateHandle(mapOf()), repository)
        }
    }

    @Test
    fun `send ignores a blank draft`() = runTest {
        coEvery { repository.messages("c1") } returns Outcome.Success(emptyList())
        val vm = ConversationThreadViewModel(SavedStateHandle(mapOf("conversationId" to "c1")), repository)
        vm.draft = "   "

        vm.send()

        coVerify(exactly = 0) { repository.sendMessage(any(), any()) }
    }

    @Test
    fun `a successful send trims the draft, clears it and reloads messages`() = runTest {
        coEvery { repository.messages("c1") } returnsMany listOf(
            Outcome.Success(emptyList()),
            Outcome.Success(listOf(Message("m1", "c1", "u1", "Hi there", "2026-07-16T00:00:00Z"))),
        )
        coEvery { repository.sendMessage("c1", "Hi there") } returns
            Outcome.Success(Message("m1", "c1", "u1", "Hi there", "2026-07-16T00:00:00Z"))
        val vm = ConversationThreadViewModel(SavedStateHandle(mapOf("conversationId" to "c1")), repository)
        vm.draft = "  Hi there  "

        vm.send()

        assertEquals("", vm.draft)
        assertFalse(vm.sending)
        assertEquals(1, vm.state.messages.size)
    }

    @Test
    fun `a failed send keeps the draft and surfaces the error`() = runTest {
        coEvery { repository.messages("c1") } returns Outcome.Success(emptyList())
        coEvery { repository.sendMessage("c1", "Hi") } returns Outcome.Failure("Blocked")
        val vm = ConversationThreadViewModel(SavedStateHandle(mapOf("conversationId" to "c1")), repository)
        vm.draft = "Hi"

        vm.send()

        assertEquals("Hi", vm.draft)
        assertEquals("Blocked", vm.state.error)
    }
}
