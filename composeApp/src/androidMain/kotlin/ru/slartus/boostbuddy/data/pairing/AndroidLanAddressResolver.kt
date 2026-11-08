package ru.slartus.boostbuddy.data.pairing

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

internal class AndroidLanAddressResolver(context: Context) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    fun resolve(): String? = fromConnectivityManager() ?: fromNetworkInterfaces()

    @Suppress("DEPRECATION")
    private fun fromConnectivityManager(): String? = runCatching {
        val manager = connectivityManager ?: return null
        val networks = listOfNotNull(manager.activeNetwork) + manager.allNetworks
        networks.distinct()
            .filter { manager.isLocalTransport(it) }
            .firstNotNullOfOrNull { network ->
                manager.getLinkProperties(network)
                    ?.linkAddresses
                    ?.map { it.address }
                    ?.firstPrivateIpv4()
            }
    }.getOrNull()

    private fun ConnectivityManager.isLocalTransport(network: Network): Boolean {
        val capabilities = getNetworkCapabilities(network) ?: return false
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun fromNetworkInterfaces(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual && !it.isPointToPoint }
            .firstNotNullOfOrNull { it.inetAddresses.toList().firstPrivateIpv4() }
    }.getOrNull()

    private fun List<InetAddress>.firstPrivateIpv4(): String? =
        filterIsInstance<Inet4Address>()
            .mapNotNull { it.hostAddress }
            .firstOrNull { isPrivateIpv4(it) }
}
