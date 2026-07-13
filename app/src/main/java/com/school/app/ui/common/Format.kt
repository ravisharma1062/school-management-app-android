package com.school.app.ui.common

import androidx.compose.ui.graphics.Color
import com.school.app.domain.model.AttendanceStatus
import com.school.app.domain.model.BookIssueStatus
import com.school.app.domain.model.FeeStatus
import com.school.app.domain.model.LeaveStatus
import com.school.app.domain.model.SchoolStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

fun formatDate(iso: String): String = try {
    LocalDate.parse(iso).format(dateFormatter)
} catch (_: Exception) {
    iso
}

fun formatDateTime(iso: String): String = try {
    OffsetDateTime.parse(iso).toLocalDate().format(dateFormatter)
} catch (_: Exception) {
    try {
        LocalDateTime.parse(iso).toLocalDate().format(dateFormatter)
    } catch (_: Exception) {
        iso
    }
}

fun formatMoney(amount: Double): String = "₹%,.2f".format(Locale.ENGLISH, amount)

fun AttendanceStatus.color(): Color = when (this) {
    AttendanceStatus.PRESENT -> Color(0xFF15803D)
    AttendanceStatus.ABSENT -> Color(0xFFB91C1C)
    AttendanceStatus.LATE -> Color(0xFFB45309)
    AttendanceStatus.EXCUSED -> Color(0xFF1D4ED8)
}

fun FeeStatus.color(): Color = when (this) {
    FeeStatus.PAID -> Color(0xFF15803D)
    FeeStatus.PARTIAL -> Color(0xFFB45309)
    FeeStatus.PENDING -> Color(0xFF6B7280)
    FeeStatus.OVERDUE -> Color(0xFFB91C1C)
}

fun LeaveStatus.color(): Color = when (this) {
    LeaveStatus.APPROVED -> Color(0xFF15803D)
    LeaveStatus.REJECTED -> Color(0xFFB91C1C)
    LeaveStatus.PENDING -> Color(0xFFB45309)
}

fun BookIssueStatus.color(): Color = when (this) {
    BookIssueStatus.ISSUED -> Color(0xFF1D4ED8)
    BookIssueStatus.RETURNED -> Color(0xFF15803D)
}

fun SchoolStatus.color(): Color = when (this) {
    SchoolStatus.TRIAL -> Color(0xFFB45309)
    SchoolStatus.ACTIVE -> Color(0xFF15803D)
    SchoolStatus.PAST_DUE -> Color(0xFFB45309)
    SchoolStatus.SUSPENDED -> Color(0xFFB91C1C)
    SchoolStatus.CANCELLED -> Color(0xFF6B7280)
}
