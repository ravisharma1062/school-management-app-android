package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.payment.PaymentOutcome
import com.school.app.data.payment.RazorpayResultBus
import com.school.app.data.repository.ExamResultRepository
import com.school.app.data.repository.FeeRepository
import com.school.app.data.repository.NoticeRepository
import com.school.app.data.repository.PaymentRepository
import com.school.app.domain.model.ExamResult
import com.school.app.domain.model.Fee
import com.school.app.domain.model.Notice
import com.school.app.domain.model.PaymentInitiateResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExamResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val examResultRepository: ExamResultRepository,
) : ViewModel() {

    private val studentId: String = checkNotNull(savedStateHandle["studentId"])
    val studentName: String = savedStateHandle["name"] ?: ""

    var state by mutableStateOf<UiState<List<ExamResult>>>(UiState.Loading)
        private set

    var downloading by mutableStateOf(false)
        private set
    var downloadError by mutableStateOf<String?>(null)
        private set
    var downloadedFile by mutableStateOf<File?>(null)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            state = examResultRepository.forStudent(studentId).toUiState()
        }
    }

    fun downloadReportCard() {
        if (downloading) return
        downloading = true
        downloadError = null
        viewModelScope.launch {
            when (val result = examResultRepository.downloadReportCard(studentId)) {
                is Outcome.Success -> downloadedFile = result.data
                is Outcome.Failure -> downloadError = result.message
            }
            downloading = false
        }
    }

    fun consumeDownloadedFile() {
        downloadedFile = null
    }
}

sealed interface PaymentUiState {
    data object Idle : PaymentUiState
    data class Initiating(val feeId: String) : PaymentUiState
    data class ReadyToOpen(val feeId: String, val order: PaymentInitiateResponse) : PaymentUiState
    data object Success : PaymentUiState
    data class Error(val message: String) : PaymentUiState
}

@HiltViewModel
class FeesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val feeRepository: FeeRepository,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {

    private val studentId: String = checkNotNull(savedStateHandle["studentId"])
    val studentName: String = savedStateHandle["name"] ?: ""

    var state by mutableStateOf<UiState<List<Fee>>>(UiState.Loading)
        private set

    var paymentState by mutableStateOf<PaymentUiState>(PaymentUiState.Idle)
        private set

    init {
        load()
        viewModelScope.launch {
            RazorpayResultBus.events.collect { outcome ->
                paymentState = when (outcome) {
                    is PaymentOutcome.Success -> PaymentUiState.Success.also { load() }
                    is PaymentOutcome.Error -> PaymentUiState.Error(outcome.message)
                }
            }
        }
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            state = feeRepository.forStudent(studentId).toUiState()
        }
    }

    fun payFee(feeId: String) {
        paymentState = PaymentUiState.Initiating(feeId)
        viewModelScope.launch {
            paymentState = when (val result = paymentRepository.initiate(feeId)) {
                is Outcome.Success -> PaymentUiState.ReadyToOpen(feeId, result.data)
                is Outcome.Failure -> PaymentUiState.Error(result.message)
            }
        }
    }

    /** Called once the checkout sheet has actually been launched, so we don't reopen it on recomposition. */
    fun onCheckoutOpened() {
        paymentState = PaymentUiState.Idle
    }

    fun dismissPaymentMessage() {
        paymentState = PaymentUiState.Idle
    }
}

@HiltViewModel
class NoticesViewModel @Inject constructor(
    private val noticeRepository: NoticeRepository,
) : ViewModel() {

    data class State(
        val items: List<Notice> = emptyList(),
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val endReached: Boolean = false,
        val error: String? = null,
        val page: Int = 0,
    )

    var state by mutableStateOf(State())
        private set

    init {
        refresh()
    }

    fun refresh() {
        state = State(loading = true)
        loadPage(0)
    }

    fun loadNext() {
        if (state.loading || state.loadingMore || state.endReached) return
        state = state.copy(loadingMore = true)
        loadPage(state.page + 1)
    }

    private fun loadPage(page: Int) {
        viewModelScope.launch {
            when (val result = noticeRepository.page(page)) {
                is Outcome.Success -> state = state.copy(
                    items = if (page == 0) result.data.content else state.items + result.data.content,
                    loading = false,
                    loadingMore = false,
                    endReached = result.data.last,
                    error = null,
                    page = page,
                )
                is Outcome.Failure -> state = state.copy(
                    loading = false,
                    loadingMore = false,
                    error = result.message,
                )
            }
        }
    }
}
