package ru.slartus.boostbuddy.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersistableStackTest {

    private fun persist(vararg items: String) = persistableStack(items.toList()) { it == TRANSIENT }

    @Test
    fun noTransient_keepsWholeStack() {
        assertEquals(listOf("Main", "Blog"), persist("Main", "Blog"))
    }

    @Test
    fun transientOnTop_dropsOnlyIt() {
        assertEquals(listOf("Main", "Blog"), persist("Main", "Blog", TRANSIENT))
    }

    @Test
    fun transientInMiddle_dropsItAndEverythingAbove() {
        assertEquals(listOf("Main"), persist("Main", TRANSIENT, "Video"))
    }

    @Test
    fun transientOnly_returnsNullForInitialStack() {
        assertNull(persist(TRANSIENT))
    }

    @Test
    fun transientAtBottom_returnsNullForInitialStack() {
        assertNull(persist(TRANSIENT, "Main"))
    }

    private companion object {
        const val TRANSIENT = "PairSend"
    }
}
