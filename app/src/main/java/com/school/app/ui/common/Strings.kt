package com.school.app.ui.common

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.school.app.domain.model.LanguageCode
import java.util.Locale

/** Provided once at the nav-host root from [com.school.app.data.auth.Session.preferredLanguage]. */
val LocalLanguage = compositionLocalOf { LanguageCode.EN }

private fun localizedResources(context: Context, lang: LanguageCode): Resources {
    val locale = if (lang == LanguageCode.HI) Locale("hi") else Locale.ENGLISH
    val config = android.content.res.Configuration(context.resources.configuration)
    config.setLocale(locale)
    return context.createConfigurationContext(config).resources
}

/**
 * Resolves a string resource in the current [LocalLanguage] regardless of the device's
 * system locale, since language here is a per-user app preference, not a system setting.
 */
@Composable
fun stringRes(@StringRes id: Int, vararg formatArgs: Any): String {
    val context = LocalContext.current
    val lang = LocalLanguage.current
    val resources = remember(lang) { localizedResources(context, lang) }
    return if (formatArgs.isEmpty()) resources.getString(id) else resources.getString(id, *formatArgs)
}

@Composable
fun roleLabel(roleName: String): String {
    val id = when (roleName) {
        "TEACHER" -> com.school.app.R.string.role_teacher
        "PARENT" -> com.school.app.R.string.role_parent
        else -> com.school.app.R.string.role_admin
    }
    return stringRes(id)
}
