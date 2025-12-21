package com.github.sukieva.gitcommitstats.toolwindow.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class GitQueryServiceTest {

    @Test
    fun `normalizeDateRange rounds to start and end of day`() {
        val startCalendar = Calendar.getInstance().apply {
            set(2024, Calendar.MAY, 10, 15, 30, 45)
        }
        val endCalendar = Calendar.getInstance().apply {
            set(2024, Calendar.MAY, 12, 3, 5, 0)
        }

        val (start, end) = normalizeDateRange(startCalendar.time, endCalendar.time)

        requireNotNull(start)
        requireNotNull(end)

        val startResult = Calendar.getInstance().apply { time = start }
        val endResult = Calendar.getInstance().apply { time = end }

        assertEquals(0, startResult.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startResult.get(Calendar.MINUTE))
        assertEquals(0, startResult.get(Calendar.SECOND))
        assertEquals(0, startResult.get(Calendar.MILLISECOND))

        assertEquals(23, endResult.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endResult.get(Calendar.MINUTE))
        assertEquals(59, endResult.get(Calendar.SECOND))
        assertEquals(999, endResult.get(Calendar.MILLISECOND))
    }

    @Test
    fun `normalizeDateRange returns nulls when both dates absent`() {
        val (start, end) = normalizeDateRange(null, null)

        assertNull(start)
        assertNull(end)
    }
}
