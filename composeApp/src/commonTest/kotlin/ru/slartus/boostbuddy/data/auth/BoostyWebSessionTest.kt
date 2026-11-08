package ru.slartus.boostbuddy.data.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BoostyWebSessionTest {

    @Test
    fun parseCookieHeader_splitsPairsAndTrimsWhitespace() {
        val result = parseCookieHeader("  a=1 ;b = 2;   c=3  ")

        assertEquals(mapOf("a" to "1", "b" to "2", "c" to "3"), result)
    }

    @Test
    fun parseCookieHeader_keepsEqualsSignsInValue() {
        val result = parseCookieHeader("token=abc==; q=x=y=z")

        assertEquals("abc==", result["token"])
        assertEquals("x=y=z", result["q"])
    }

    @Test
    fun parseCookieHeader_skipsPartsWithoutNameOrEquals() {
        val result = parseCookieHeader("flag; =orphan; ;a=1")

        assertEquals(mapOf("a" to "1"), result)
    }

    @Test
    fun parseCookieHeader_keepsEmptyValue() {
        val result = parseCookieHeader("empty=; a=1")

        assertEquals("", result["empty"])
        assertEquals("1", result["a"])
    }

    @Test
    fun parseCookieHeader_returnsEmptyMapForBlankInput() {
        assertEquals(emptyMap(), parseCookieHeader(""))
        assertEquals(emptyMap(), parseCookieHeader("   "))
    }

    @Test
    fun boostyClientId_returnsPlainValue() {
        val cookies = parseCookieHeader("auth=x; _clientId=bcec210b-64d5-4c95-9d6f-da9ac48e7441")

        assertEquals("bcec210b-64d5-4c95-9d6f-da9ac48e7441", cookies.boostyClientId())
    }

    @Test
    fun boostyClientId_removesSurroundingQuotes() {
        val cookies = parseCookieHeader("_clientId=\"abc-123\"")

        assertEquals("abc-123", cookies.boostyClientId())
    }

    @Test
    fun boostyClientId_decodesUrlEncodedQuotesAndValue() {
        val cookies = parseCookieHeader("_clientId=%22abc%2D123%22")

        assertEquals("abc-123", cookies.boostyClientId())
    }

    @Test
    fun boostyClientId_trimsWhitespaceInsideQuotes() {
        val cookies = mapOf("_clientId" to " \" abc-123 \" ")

        assertEquals("abc-123", cookies.boostyClientId())
    }

    @Test
    fun boostyClientId_returnsNullForEmptyValue() {
        assertNull(parseCookieHeader("_clientId=").boostyClientId())
        assertNull(parseCookieHeader("_clientId=\"\"").boostyClientId())
        assertNull(parseCookieHeader("_clientId=%22%22").boostyClientId())
    }

    @Test
    fun boostyClientId_returnsNullWhenMissing() {
        assertNull(parseCookieHeader("auth=x; other=y").boostyClientId())
    }
}
