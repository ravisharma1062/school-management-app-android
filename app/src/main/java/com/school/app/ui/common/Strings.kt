package com.school.app.ui.common

import com.school.app.domain.model.LanguageCode

/**
 * Lightweight app-chrome translations (home screen, top bars, common actions).
 * Deep screen-level content stays English-only — same scope boundary as the web app's i18n rollout.
 */
private val EN: Map<String, String> = mapOf(
    "app.title" to "School App",
    "common.logOut" to "Log out",
    "common.syncNow" to "Sync now",
    "common.pendingSync" to "%d attendance mark%s waiting to sync",
    "home.hello" to "Hello, %s",
    "role.ADMIN" to "Admin",
    "role.TEACHER" to "Teacher",
    "role.PARENT" to "Parent",
    "feature.markAttendance" to "Mark Attendance",
    "feature.students" to "Students",
    "feature.myChildren" to "My Children",
    "feature.timetable" to "Timetable",
    "feature.homework" to "Homework",
    "feature.notices" to "Notices",
    "feature.leaveRequests" to "Leave Requests",
    "feature.events" to "Events",
    "feature.messages" to "Messages",
)

private val HI: Map<String, String> = mapOf(
    "app.title" to "स्कूल ऐप",
    "common.logOut" to "लॉग आउट",
    "common.syncNow" to "अभी सिंक करें",
    "common.pendingSync" to "%d उपस्थिति प्रविष्टियां सिंक होने की प्रतीक्षा में",
    "home.hello" to "नमस्ते, %s",
    "role.ADMIN" to "प्रशासक",
    "role.TEACHER" to "शिक्षक",
    "role.PARENT" to "अभिभावक",
    "feature.markAttendance" to "उपस्थिति दर्ज करें",
    "feature.students" to "छात्र",
    "feature.myChildren" to "मेरे बच्चे",
    "feature.timetable" to "समय सारिणी",
    "feature.homework" to "गृहकार्य",
    "feature.notices" to "सूचनाएं",
    "feature.leaveRequests" to "अवकाश अनुरोध",
    "feature.events" to "कार्यक्रम",
    "feature.messages" to "संदेश",
)

fun t(lang: LanguageCode, key: String, vararg args: Any): String {
    val template = (if (lang == LanguageCode.HI) HI else EN)[key] ?: EN[key] ?: key
    return if (args.isEmpty()) template else String.format(template, *args)
}
