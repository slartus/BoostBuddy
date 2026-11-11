package ru.slartus.boostbuddy.components.pairsend

import ru.slartus.boostbuddy.data.pairing.PairingBase64
import ru.slartus.boostbuddy.data.pairing.PairingLink
import ru.slartus.boostbuddy.data.pairing.PairingTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class PairSendDeepLinkTest {

    private val key = ByteArray(32) { it.toByte() }
    private val sid = PairingBase64.encode(ByteArray(16) { (0xa0 + it).toByte() })
    private val target = PairingTarget(host = "192.168.1.10", port = 8765, sid = sid, key = key)
    private val link = PairingLink.appDeepLink(target)

    @Test
    fun validLink_loggedIn_opensPairSendWithTarget() {
        assertEquals(
            PairSendDeepLinkDecision.Open(target),
            resolvePairSendDeepLink(link, allowLoopback = false, isLoggedIn = true),
        )
    }

    @Test
    fun validLink_withKeyInQuery_opensPairSend() {
        val queryLink = "boostbuddy://pair?h=192.168.1.10:8765&s=$sid&k=${PairingBase64.encode(key)}"
        assertEquals(
            PairSendDeepLinkDecision.Open(target),
            resolvePairSendDeepLink(queryLink, allowLoopback = false, isLoggedIn = true),
        )
    }

    @Test
    fun validLink_notLoggedIn_reportsNotLoggedIn() {
        assertEquals(
            PairSendDeepLinkDecision.NotLoggedIn,
            resolvePairSendDeepLink(link, allowLoopback = false, isLoggedIn = false),
        )
    }

    @Test
    fun invalidLink_reportsInvalidEvenWhenNotLoggedIn() {
        listOf(
            "boostbuddy://pair?h=192.168.1.10:8765&s=$sid",
            "boostbuddy://other?h=192.168.1.10:8765&s=$sid#k=${PairingBase64.encode(key)}",
            "boostbuddy://pair?h=8.8.8.8:8765&s=$sid#k=${PairingBase64.encode(key)}",
            "garbage",
        ).forEach { url ->
            assertEquals(
                PairSendDeepLinkDecision.InvalidLink,
                resolvePairSendDeepLink(url, allowLoopback = false, isLoggedIn = false),
            )
            assertEquals(
                PairSendDeepLinkDecision.InvalidLink,
                resolvePairSendDeepLink(url, allowLoopback = false, isLoggedIn = true),
            )
        }
    }

    @Test
    fun loopbackLink_acceptedOnlyWhenAllowed() {
        val loopback = PairingLink.appDeepLink(target.copy(host = "127.0.0.1"))
        assertEquals(
            PairSendDeepLinkDecision.InvalidLink,
            resolvePairSendDeepLink(loopback, allowLoopback = false, isLoggedIn = true),
        )
        assertEquals(
            PairSendDeepLinkDecision.Open(target.copy(host = "127.0.0.1")),
            resolvePairSendDeepLink(loopback, allowLoopback = true, isLoggedIn = true),
        )
    }
}
