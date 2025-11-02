package com.lettingin.intervalAlarm.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/**
 * Unit tests for TimeFormatter utility.
 * Tests 24-hour format conversion for various time scenarios.
 */
class TimeFormatterTest {
    
    @Test
    fun `format24Hour formats midnight correctly`() {
        val time = LocalTime.of(0, 0)
        assertEquals("00:00", TimeFormatter.format24Hour(time))
    }
    
    @Test
    fun `format24Hour formats noon correctly`() {
        val time = LocalTime.of(12, 0)
        assertEquals("12:00", TimeFormatter.format24Hour(time))
    }
    
    @Test
    fun `format24Hour formats morning time correctly`() {
        val time = LocalTime.of(9, 30)
        assertEquals("09:30", TimeFormatter.format24Hour(time))
    }
    
    @Test
    fun `format24Hour formats afternoon time correctly`() {
        val time = LocalTime.of(15, 30)
        assertEquals("15:30", TimeFormatter.format24Hour(time))
    }
    
    @Test
    fun `format24Hour formats evening time correctly`() {
        val time = LocalTime.of(23, 59)
        assertEquals("23:59", TimeFormatter.format24Hour(time))
    }
    
    @Test
    fun `format24Hour formats single digit minutes with leading zero`() {
        val time = LocalTime.of(10, 5)
        assertEquals("10:05", TimeFormatter.format24Hour(time))
    }
    
    @Test
    fun `formatTimeRange formats morning to afternoon range correctly`() {
        val start = LocalTime.of(9, 0)
        val end = LocalTime.of(17, 30)
        assertEquals("09:00 - 17:30", TimeFormatter.formatTimeRange(start, end))
    }
    
    @Test
    fun `formatTimeRange formats midnight to noon range correctly`() {
        val start = LocalTime.of(0, 0)
        val end = LocalTime.of(12, 0)
        assertEquals("00:00 - 12:00", TimeFormatter.formatTimeRange(start, end))
    }
    
    @Test
    fun `formatTimeRange formats evening range correctly`() {
        val start = LocalTime.of(18, 15)
        val end = LocalTime.of(23, 45)
        assertEquals("18:15 - 23:45", TimeFormatter.formatTimeRange(start, end))
    }
    
    @Test
    fun `format24Hour with timestamp formats correctly`() {
        // Create a timestamp for a specific time (e.g., 15:30:00 on some date)
        // Using epoch milliseconds for 2024-01-01 15:30:00 UTC
        val timestamp = 1704122400000L // This is approximately 2024-01-01 12:00:00 UTC
        
        // Note: The actual formatted time will depend on system timezone
        // This test verifies the method doesn't throw and returns proper format
        val result = TimeFormatter.format24Hour(timestamp)
        
        // Verify format is HH:mm (5 characters with colon in middle)
        assertEquals(5, result.length)
        assertEquals(':', result[2])
        
        // Verify hours are in valid range (00-23)
        val hours = result.substring(0, 2).toInt()
        assert(hours in 0..23)
        
        // Verify minutes are in valid range (00-59)
        val minutes = result.substring(3, 5).toInt()
        assert(minutes in 0..59)
    }
}
