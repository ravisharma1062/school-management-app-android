package com.school.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.payment.PaymentOutcome
import com.school.app.data.payment.RazorpayResultBus
import com.school.app.data.repository.ExamResultRepository
import com.school.app.data.repository.FeeRepository
import com.school.app.data.repository.PaymentRepository
import com.school.app.domain.model.Fee
import com.school.app.domain.model.FeeStatus
import com.school.app.domain.model.PaymentInitiateResponse
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.ExamResultsViewModel
import com.school.app.viewmodel.FeesViewModel
import com.school.app.viewmodel.PaymentUiState
import com.school.app.viewmodel.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class MiscViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // --- ExamResultsViewModel ---

    private val examResultRepository = mockk<ExamResultRepository>()

    @Test
    fun `exam results downloads a report card and exposes the file`() = runTest {
        coEvery { examResultRepository.forStudent("s1") } returns Outcome.Success(emptyList())
        val file = File("report.pdf")
        coEvery { examResultRepository.downloadReportCard("s1") } returns Outcome.Success(file)
        val vm = ExamResultsViewModel(SavedStateHandle(mapOf("studentId" to "s1")), examResultRepository)

        vm.downloadReportCard()

        assertEquals(file, vm.downloadedFile)
        assertFalse(vm.downloading)
        assertNull(vm.downloadError)
    }

    @Test
    fun `a failed download surfaces the error and clears any stale file`() = runTest {
        coEvery { examResultRepository.forStudent("s1") } returns Outcome.Success(emptyList())
        coEvery { examResultRepository.downloadReportCard("s1") } returns Outcome.Failure("Not available yet")
        val vm = ExamResultsViewModel(SavedStateHandle(mapOf("studentId" to "s1")), examResultRepository)

        vm.downloadReportCard()

        assertEquals("Not available yet", vm.downloadError)
        assertNull(vm.downloadedFile)
    }

    @Test
    fun `consumeDownloadedFile clears the reference`() = runTest {
        coEvery { examResultRepository.forStudent("s1") } returns Outcome.Success(emptyList())
        coEvery { examResultRepository.downloadReportCard("s1") } returns Outcome.Success(File("x.pdf"))
        val vm = ExamResultsViewModel(SavedStateHandle(mapOf("studentId" to "s1")), examResultRepository)
        vm.downloadReportCard()

        vm.consumeDownloadedFile()

        assertNull(vm.downloadedFile)
    }

    @Test
    fun `downloading a second report card while one is in flight is ignored`() = runTest {
        coEvery { examResultRepository.forStudent("s1") } returns Outcome.Success(emptyList())
        val gate = kotlinx.coroutines.CompletableDeferred<Outcome<File>>()
        coEvery { examResultRepository.downloadReportCard("s1") } coAnswers { gate.await() }
        val vm = ExamResultsViewModel(SavedStateHandle(mapOf("studentId" to "s1")), examResultRepository)

        vm.downloadReportCard()
        assertTrue(vm.downloading)
        vm.downloadReportCard()
        gate.complete(Outcome.Success(File("x.pdf")))

        coVerify(exactly = 1) { examResultRepository.downloadReportCard(any()) }
    }

    // --- FeesViewModel ---

    private val feeRepository = mockk<FeeRepository>()
    private val paymentRepository = mockk<PaymentRepository>()

    private fun fee(id: String) = Fee(id, "s1", "Term 1", 5000.0, 0.0, FeeStatus.PENDING, "2026-08-01")

    // FeesViewModel.init subscribes to RazorpayResultBus.events with a collect{} that runs
    // forever in production (cancelled only by ViewModel.onCleared(), which nothing calls in a
    // test). Cancelling viewModelScope after each test releases that coroutine immediately
    // instead of leaking it — and its mocks/proxies — for the rest of the test JVM's life.

    @Test
    fun `fees loads the student's fees`() = runTest {
        coEvery { feeRepository.forStudent("s1") } returns Outcome.Success(listOf(fee("f1")))
        val vm = FeesViewModel(SavedStateHandle(mapOf("studentId" to "s1")), feeRepository, paymentRepository)

        assertEquals(UiState.Ready(listOf(fee("f1")), false), vm.state)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `payFee moves through initiating to ready to open on success`() = runTest {
        coEvery { feeRepository.forStudent("s1") } returns Outcome.Success(emptyList())
        val order = PaymentInitiateResponse("order1", 500000, "INR", "key1")
        coEvery { paymentRepository.initiate("f1") } returns Outcome.Success(order)
        val vm = FeesViewModel(SavedStateHandle(mapOf("studentId" to "s1")), feeRepository, paymentRepository)

        vm.payFee("f1")

        assertEquals(PaymentUiState.ReadyToOpen("f1", order), vm.paymentState)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a failed payment initiation surfaces an error state`() = runTest {
        coEvery { feeRepository.forStudent("s1") } returns Outcome.Success(emptyList())
        coEvery { paymentRepository.initiate("f1") } returns Outcome.Failure("Gateway unavailable")
        val vm = FeesViewModel(SavedStateHandle(mapOf("studentId" to "s1")), feeRepository, paymentRepository)

        vm.payFee("f1")

        assertEquals(PaymentUiState.Error("Gateway unavailable"), vm.paymentState)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `onCheckoutOpened and dismissPaymentMessage both reset to idle`() = runTest {
        coEvery { feeRepository.forStudent("s1") } returns Outcome.Success(emptyList())
        coEvery { paymentRepository.initiate("f1") } returns
            Outcome.Success(PaymentInitiateResponse("o1", 100, "INR", "k1"))
        val vm = FeesViewModel(SavedStateHandle(mapOf("studentId" to "s1")), feeRepository, paymentRepository)
        vm.payFee("f1")

        vm.onCheckoutOpened()
        assertEquals(PaymentUiState.Idle, vm.paymentState)

        vm.payFee("f1")
        vm.dismissPaymentMessage()
        assertEquals(PaymentUiState.Idle, vm.paymentState)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a razorpay success event flips to Success and reloads fees`() = runTest {
        coEvery { feeRepository.forStudent("s1") } returns Outcome.Success(emptyList())
        val vm = FeesViewModel(SavedStateHandle(mapOf("studentId" to "s1")), feeRepository, paymentRepository)

        RazorpayResultBus.emit(PaymentOutcome.Success("pay1", "order1", "sig1"))

        assertEquals(PaymentUiState.Success, vm.paymentState)
        coVerify(atLeast = 2) { feeRepository.forStudent("s1") } // once on init, once after the event
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a razorpay error event surfaces its message without reloading`() = runTest {
        coEvery { feeRepository.forStudent("s1") } returns Outcome.Success(emptyList())
        val vm = FeesViewModel(SavedStateHandle(mapOf("studentId" to "s1")), feeRepository, paymentRepository)

        RazorpayResultBus.emit(PaymentOutcome.Error("Payment cancelled"))

        assertEquals(PaymentUiState.Error("Payment cancelled"), vm.paymentState)
        vm.viewModelScope.cancel()
    }
}
