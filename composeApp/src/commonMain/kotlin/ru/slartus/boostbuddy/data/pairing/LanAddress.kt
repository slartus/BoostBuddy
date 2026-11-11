package ru.slartus.boostbuddy.data.pairing

internal fun isPrivateIpv4(host: String, allowLoopback: Boolean = false): Boolean {
    val octets = parseIpv4(host) ?: return false
    val first = octets[0]
    val second = octets[1]
    return first == 10 ||
            (first == 172 && second in 16..31) ||
            (first == 192 && second == 168) ||
            (allowLoopback && first == 127)
}

private fun parseIpv4(host: String): List<Int>? {
    val parts = host.split('.')
    if (parts.size != 4) return null
    return parts.map { part ->
        if (part.isEmpty() || part.length > 3) return null
        if (!part.all { it in '0'..'9' }) return null
        if (part.length > 1 && part[0] == '0') return null
        part.toInt().takeIf { it <= 255 } ?: return null
    }
}
