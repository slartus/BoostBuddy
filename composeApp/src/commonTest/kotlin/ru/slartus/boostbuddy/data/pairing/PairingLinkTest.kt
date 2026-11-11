package ru.slartus.boostbuddy.data.pairing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class PairingLinkTest {

    private val key = ByteArray(32) { it.toByte() }
    private val keyText = PairingBase64.encode(key)
    private val sid = PairingBase64.encode(ByteArray(16) { (0xa0 + it).toByte() })
    private val target = PairingTarget(host = "192.168.1.10", port = 8765, sid = sid, key = key)

    @Test
    fun receiverPageUrl_hasKeyAndSidInFragment() {
        assertEquals(
            "http://192.168.1.10:8765/p#k=$keyText&s=$sid",
            PairingLink.receiverPageUrl(target),
        )
    }

    @Test
    fun appDeepLink_hasKeyInFragment() {
        assertEquals(
            "boostbuddy://pair?h=192.168.1.10:8765&s=$sid#k=$keyText",
            PairingLink.appDeepLink(target),
        )
    }

    @Test
    fun parse_roundtripsBothFormats() {
        assertEquals(target, PairingLink.parse(PairingLink.receiverPageUrl(target)))
        assertEquals(target, PairingLink.parse(PairingLink.appDeepLink(target)))
    }

    @Test
    fun parse_appLinkAcceptsKeyFromQueryWhenFragmentMissing() {
        assertEquals(
            target,
            PairingLink.parse("boostbuddy://pair?h=192.168.1.10:8765&s=$sid&k=$keyText"),
        )
    }

    @Test
    fun parse_appLinkPrefersFragmentKey() {
        val otherKey = PairingBase64.encode(ByteArray(32) { 7 })

        assertEquals(
            target,
            PairingLink.parse("boostbuddy://pair?h=192.168.1.10:8765&s=$sid&k=$otherKey#k=$keyText"),
        )
    }

    @Test
    fun parse_receiverUrlIgnoresKeyInQuery() {
        assertNull(PairingLink.parse("http://192.168.1.10:8765/p?k=$keyText#s=$sid"))
        assertNull(PairingLink.parse("http://192.168.1.10:8765/p?k=$keyText&s=$sid"))
    }

    @Test
    fun parse_rejectsWrongKeyLength() {
        val shortKey = PairingBase64.encode(ByteArray(16))
        val longKey = PairingBase64.encode(ByteArray(33))

        assertNull(PairingLink.parse("http://192.168.1.10:8765/p#k=$shortKey&s=$sid"))
        assertNull(PairingLink.parse("boostbuddy://pair?h=192.168.1.10:8765&s=$sid#k=$longKey"))
        assertNull(PairingLink.parse("boostbuddy://pair?h=192.168.1.10:8765&s=$sid#k=$keyText=="))
    }

    @Test
    fun parse_rejectsWrongSid() {
        assertNull(PairingLink.parse("http://192.168.1.10:8765/p#k=$keyText&s=abc"))
        assertNull(PairingLink.parse("http://192.168.1.10:8765/p#k=$keyText"))
    }

    @Test
    fun parse_rejectsPublicOrLoopbackHost() {
        assertNull(PairingLink.parse("http://8.8.8.8:8765/p#k=$keyText&s=$sid"))
        assertNull(PairingLink.parse("boostbuddy://pair?h=example.com:8765&s=$sid#k=$keyText"))
        assertNull(PairingLink.parse("boostbuddy://pair?h=127.0.0.1:8765&s=$sid#k=$keyText"))
    }

    @Test
    fun parse_acceptsLoopbackWhenAllowed() {
        val parsed = PairingLink.parse(
            "boostbuddy://pair?h=127.0.0.1:8765&s=$sid#k=$keyText",
            allowLoopback = true,
        )

        assertEquals("127.0.0.1", parsed?.host)
    }

    @Test
    fun parse_rejectsBadPortAndShape() {
        assertNull(PairingLink.parse("http://192.168.1.10/p#k=$keyText&s=$sid"))
        assertNull(PairingLink.parse("http://192.168.1.10:0/p#k=$keyText&s=$sid"))
        assertNull(PairingLink.parse("http://192.168.1.10:65536/p#k=$keyText&s=$sid"))
        assertNull(PairingLink.parse("http://192.168.1.10:8765/q#k=$keyText&s=$sid"))
        assertNull(PairingLink.parse("https://192.168.1.10:8765/p#k=$keyText&s=$sid"))
        assertNull(PairingLink.parse("boostbuddy://other?h=192.168.1.10:8765&s=$sid#k=$keyText"))
        assertNull(PairingLink.parse("boostbuddy://pair?h=192.168.1.10:8765&s=$sid&s=$sid#k=$keyText"))
        assertNull(PairingLink.parse("not a link"))
    }

    @Test
    fun target_toStringDoesNotLeakKey() {
        assertFalse(target.toString().contains(keyText))
    }
}
