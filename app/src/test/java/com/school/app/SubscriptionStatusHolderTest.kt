package com.school.app

import com.school.app.data.remote.SubscriptionStatusHolder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionStatusHolderTest {

    @Test
    fun `starts clean`() {
        val holder = SubscriptionStatusHolder()
        assertFalse(holder.isSuspended.value)
        assertFalse(holder.isPastDue.value)
    }

    @Test
    fun `marks are sticky until reset`() {
        val holder = SubscriptionStatusHolder()
        holder.markSuspended()
        holder.markPastDue()
        assertTrue(holder.isSuspended.value)
        assertTrue(holder.isPastDue.value)

        holder.reset()
        assertFalse(holder.isSuspended.value)
        assertFalse(holder.isPastDue.value)
    }

    @Test
    fun `dismissing past due does not clear suspension`() {
        val holder = SubscriptionStatusHolder()
        holder.markSuspended()
        holder.markPastDue()

        holder.dismissPastDue()
        assertFalse(holder.isPastDue.value)
        assertTrue(holder.isSuspended.value)
    }
}
