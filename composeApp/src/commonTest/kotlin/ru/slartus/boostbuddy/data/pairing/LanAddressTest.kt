package ru.slartus.boostbuddy.data.pairing

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanAddressTest {

    @Test
    fun acceptsPrivateRanges() {
        listOf(
            "10.0.0.1",
            "10.255.255.255",
            "172.16.0.1",
            "172.31.255.254",
            "192.168.0.1",
            "192.168.255.255",
        ).forEach { assertTrue(isPrivateIpv4(it), it) }
    }

    @Test
    fun rejects172OutsideOf16To31() {
        assertFalse(isPrivateIpv4("172.15.255.255"))
        assertTrue(isPrivateIpv4("172.16.0.0"))
        assertTrue(isPrivateIpv4("172.31.0.0"))
        assertFalse(isPrivateIpv4("172.32.0.0"))
    }

    @Test
    fun rejectsPublicAddresses() {
        listOf(
            "8.8.8.8",
            "11.0.0.1",
            "9.255.255.255",
            "192.167.1.1",
            "192.169.1.1",
            "169.254.1.1",
            "0.0.0.0",
            "255.255.255.255",
        ).forEach { assertFalse(isPrivateIpv4(it), it) }
    }

    @Test
    fun loopbackOnlyWhenAllowed() {
        assertFalse(isPrivateIpv4("127.0.0.1"))
        assertTrue(isPrivateIpv4("127.0.0.1", allowLoopback = true))
        assertFalse(isPrivateIpv4("8.8.8.8", allowLoopback = true))
    }

    @Test
    fun rejectsGarbage() {
        listOf(
            "",
            "localhost",
            "192.168.1",
            "192.168.1.1.1",
            "192.168.1.256",
            "192.168.01.1",
            "010.0.0.1",
            "192.168.1.-1",
            "192.168.1.+1",
            "192.168..1",
            " 192.168.1.1",
            "192.168.1.1 ",
            "192.168.1.1:8080",
            "0x0a.0.0.1",
            "10.0.0.1000",
            "fd00::1",
            "١٠.0.0.1",
        ).forEach { assertFalse(isPrivateIpv4(it), it) }
    }
}
