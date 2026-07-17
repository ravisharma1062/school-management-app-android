package com.school.app

import com.school.app.ui.common.formatDate
import com.school.app.ui.common.formatDateTime
import com.school.app.ui.common.formatMoney
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `formatDate renders an iso date as day month year`() {
        assertEquals("16 Jul 2026", formatDate("2026-07-16"))
        assertEquals("01 Jan 2025", formatDate("2025-01-01"))
    }

    @Test
    fun `formatDate returns unparseable input unchanged`() {
        assertEquals("not-a-date", formatDate("not-a-date"))
        assertEquals("", formatDate(""))
        // A datetime string is not a bare date; it comes back untouched.
        assertEquals("2026-07-16T10:00:00", formatDate("2026-07-16T10:00:00"))
    }

    @Test
    fun `formatDateTime accepts an offset datetime`() {
        assertEquals("16 Jul 2026", formatDateTime("2026-07-16T10:30:00Z"))
        assertEquals("16 Jul 2026", formatDateTime("2026-07-16T01:00:00+05:30"))
    }

    @Test
    fun `formatDateTime falls back to a local datetime without offset`() {
        assertEquals("16 Jul 2026", formatDateTime("2026-07-16T10:30:00"))
    }

    @Test
    fun `formatDateTime returns unparseable input unchanged`() {
        assertEquals("soon", formatDateTime("soon"))
    }

    @Test
    fun `formatMoney uses rupee symbol grouping and two decimals`() {
        assertEquals("₹1,234.50", formatMoney(1234.5))
        assertEquals("₹0.00", formatMoney(0.0))
        // Locale.ENGLISH means western (thousands) grouping, not Indian lakh/crore grouping.
        assertEquals("₹1,000,000.00", formatMoney(1_000_000.0))
    }
}
