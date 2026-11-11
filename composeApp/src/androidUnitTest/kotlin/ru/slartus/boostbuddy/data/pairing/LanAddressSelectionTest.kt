package ru.slartus.boostbuddy.data.pairing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanAddressSelectionTest {

    @Test
    fun selectLanAddress_picksPrivateAddressOfWifiOrEthernet() {
        val networks = listOf(
            network(isLocalTransport = false, "10.64.0.12"),
            network(isLocalTransport = true, "fe80::1", "192.168.1.20"),
        )

        assertEquals("192.168.1.20", selectLanAddress(networks))
    }

    @Test
    fun selectLanAddress_skipsVpnAndPublicAddresses() {
        val networks = listOf(
            network(isLocalTransport = true, "10.8.0.2", isVpn = true),
            network(isLocalTransport = true, "100.64.1.1", "8.8.8.8"),
            network(isLocalTransport = true, "172.16.5.4"),
        )

        assertEquals("172.16.5.4", selectLanAddress(networks))
    }

    @Test
    fun selectLanAddress_returnsNullOnCellularOnly() {
        assertNull(selectLanAddress(listOf(network(isLocalTransport = false, "10.120.4.7"))))
        assertNull(selectLanAddress(emptyList()))
    }

    @Test
    fun selectInterfaceAddress_usesOnlyAllowlistedLanInterfaces() {
        val interfaces = listOf(
            networkInterface("rmnet_data0", "10.120.4.7"),
            networkInterface("tun0", "10.8.0.2"),
            networkInterface("p2p-wlan0-0", "192.168.49.1"),
            networkInterface("wlan0", "192.168.1.20"),
        )

        assertEquals("192.168.1.20", selectInterfaceAddress(interfaces))
    }

    @Test
    fun selectInterfaceAddress_acceptsEthernetHotspotAndSoftApNames() {
        assertEquals("10.0.0.5", selectInterfaceAddress(listOf(networkInterface("eth0", "10.0.0.5"))))
        assertEquals("192.168.43.1", selectInterfaceAddress(listOf(networkInterface("ap0", "192.168.43.1"))))
        assertEquals("192.168.43.1", selectInterfaceAddress(listOf(networkInterface("swlan0", "192.168.43.1"))))
    }

    @Test
    fun selectInterfaceAddress_skipsUnusableInterfaces() {
        val interfaces = listOf(
            networkInterface("wlan0", "192.168.1.20", isUsable = false),
            networkInterface("rmnet0", "10.120.4.7"),
        )

        assertNull(selectInterfaceAddress(interfaces))
    }

    private fun network(isLocalTransport: Boolean, vararg addresses: String, isVpn: Boolean = false) =
        LanNetworkCandidate(isVpn = isVpn, isLocalTransport = isLocalTransport, ipv4Addresses = addresses.toList())

    private fun networkInterface(name: String, address: String, isUsable: Boolean = true) =
        LanInterfaceCandidate(name = name, isUsable = isUsable, ipv4Addresses = listOf(address))
}
