package dev.lanyard.payload.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class LogClassifierTest {

    @Test
    fun `native ready line is ok`() {
        assertEquals(LogLevel.OK, LogClassifier.classify("native relay: pagecache_relay arm64 ready"))
    }

    @Test
    fun `verified write line is ok`() {
        assertEquals(LogLevel.OK, LogClassifier.classify("Verified: 576/576 bytes"))
        assertEquals(LogLevel.OK, LogClassifier.classify("*** PAGE CACHE MODIFIED - PAYLOAD DEPLOYED ***"))
    }

    @Test
    fun `a run that did not verify is a failure, not a success`() {
        assertEquals(
            LogLevel.FAIL,
            LogClassifier.classify("Native relay did not report a verified page-cache write."),
        )
    }

    @Test
    fun `unavailable and invalid lines are failures`() {
        assertEquals(LogLevel.FAIL, LogClassifier.classify("IpSecManager is unavailable"))
        assertEquals(LogLevel.FAIL, LogClassifier.classify("payload size 12 is invalid"))
        assertEquals(LogLevel.FAIL, LogClassifier.classify("native probe failed: UnsatisfiedLinkError"))
    }

    @Test
    fun `plain progress lines stay informational`() {
        assertEquals(LogLevel.INFO, LogClassifier.classify("asset loaded: 576 bytes"))
        assertEquals(
            LogLevel.INFO,
            LogClassifier.classify("native relay starting: target=init payload=576 bytes"),
        )
        assertEquals(LogLevel.INFO, LogClassifier.classify("=== init relay ==="))
    }

    @Test
    fun `an empty line is not given a level that would colour it`() {
        assertEquals(LogLevel.INFO, LogClassifier.classify(""))
    }
}
