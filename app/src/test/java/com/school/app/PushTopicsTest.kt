package com.school.app

import com.school.app.domain.model.Role
import com.school.app.fcm.PushTopics
import org.junit.Test

/**
 * Firebase isn't initialized on the JVM unit test classpath, so FirebaseMessaging.getInstance()
 * throws — PushTopics wraps every call in runCatching specifically so the app (and its tests)
 * keep working without google-services.json. These tests just confirm that contract holds.
 */
class PushTopicsTest {

    @Test
    fun `subscribeFor never throws even without a configured Firebase app`() {
        Role.entries.forEach { PushTopics.subscribeFor(it) }
    }

    @Test
    fun `unsubscribeAll never throws even without a configured Firebase app`() {
        PushTopics.unsubscribeAll()
    }
}
