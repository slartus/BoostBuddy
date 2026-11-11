package ru.slartus.boostbuddy.data.pairing

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

internal class AndroidLanAddressResolver(context: Context) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    fun resolve(): String? {
        val manager = connectivityManager ?: return fromNetworkInterfaces()
        val networks = try {
            networkCandidates(manager)
        } catch (e: Exception) {
            return fromNetworkInterfaces()
        }
        return selectLanAddress(networks)
    }

    @Suppress("DEPRECATION")
    private fun networkCandidates(manager: ConnectivityManager): List<LanNetworkCandidate> =
        (listOfNotNull(manager.activeNetwork) + manager.allNetworks)
            .distinct()
            .mapNotNull { network ->
                val capabilities = manager.getNetworkCapabilities(network) ?: return@mapNotNull null
                LanNetworkCandidate(
                    isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
                    isLocalTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
                    ipv4Addresses = manager.getLinkProperties(network)
                        ?.linkAddresses
                        ?.map { it.address }
                        .orEmpty()
                        .ipv4Strings(),
                )
            }

    private fun fromNetworkInterfaces(): String? = try {
        selectInterfaceAddress(
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().map { networkInterface ->
                LanInterfaceCandidate(
                    name = networkInterface.name,
                    isUsable = networkInterface.isUp &&
                            !networkInterface.isLoopback &&
                            !networkInterface.isVirtual &&
                            !networkInterface.isPointToPoint,
                    ipv4Addresses = networkInterface.inetAddresses.toList().ipv4Strings(),
                )
            }
        )
    } catch (e: Exception) {
        null
    }

    private fun List<InetAddress>.ipv4Strings(): List<String> =
        filterIsInstance<Inet4Address>().mapNotNull { it.hostAddress }
}

internal data class LanNetworkCandidate(
    val isVpn: Boolean,
    val isLocalTransport: Boolean,
    val ipv4Addresses: List<String>,
)

internal data class LanInterfaceCandidate(
    val name: String,
    val isUsable: Boolean,
    val ipv4Addresses: List<String>,
)

private val LAN_INTERFACE_NAME = Regex("^(wlan|swlan|eth|ap)\\d+$")

internal fun selectLanAddress(networks: List<LanNetworkCandidate>): String? =
    networks
        .filter { it.isLocalTransport && !it.isVpn }
        .firstNotNullOfOrNull { network -> network.ipv4Addresses.firstOrNull { isPrivateIpv4(it) } }

internal fun selectInterfaceAddress(interfaces: List<LanInterfaceCandidate>): String? =
    interfaces
        .filter { it.isUsable && LAN_INTERFACE_NAME.matches(it.name) }
        .firstNotNullOfOrNull { candidate -> candidate.ipv4Addresses.firstOrNull { isPrivateIpv4(it) } }
