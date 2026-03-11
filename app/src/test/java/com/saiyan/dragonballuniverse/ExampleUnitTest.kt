package com.saiyan.dragonballuniverse

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun resolveImageUrl_blank_usesFallback() {
        val fallback = "https://example.com/fallback.jpg"
        val url = resolveImageUrl("   ", fallback)
        assertEquals(fallback, url)
    }

    @Test
    fun resolveImageUrl_null_usesFallback() {
        val fallback = "https://example.com/fallback.jpg"
        val url = resolveImageUrl(null, fallback)
        assertEquals(fallback, url)
    }

    @Test
    fun resolveImageUrl_http_isUpgradedToHttps() {
        val fallback = "https://example.com/fallback.jpg"
        val url = resolveImageUrl("http://example.com/a.png", fallback)
        assertEquals("https://example.com/a.png", url)
    }

    @Test
    fun resolveImageUrl_https_isUnchanged() {
        val fallback = "https://example.com/fallback.jpg"
        val url = resolveImageUrl("https://example.com/a.png", fallback)
        assertEquals("https://example.com/a.png", url)
    }
}
