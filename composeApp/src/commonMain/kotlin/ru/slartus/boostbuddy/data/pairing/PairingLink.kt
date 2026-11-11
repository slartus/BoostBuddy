package ru.slartus.boostbuddy.data.pairing

import io.ktor.http.decodeURLQueryComponent

internal data class PairingTarget(
    val host: String,
    val port: Int,
    val sid: String,
    val key: ByteArray,
) {
    val authority: String get() = "$host:$port"

    override fun equals(other: Any?): Boolean =
        other is PairingTarget &&
                host == other.host &&
                port == other.port &&
                sid == other.sid &&
                key.contentEquals(other.key)

    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + port
        result = 31 * result + sid.hashCode()
        result = 31 * result + key.contentHashCode()
        return result
    }

    override fun toString(): String = "PairingTarget(host=$host, port=$port, sid=$sid)"
}

internal object PairingLink {
    const val APP_SCHEME = "boostbuddy"
    const val APP_HOST = "pair"
    const val RECEIVER_PAGE_PATH = "/p"

    private const val PARAM_HOST = "h"
    private const val PARAM_SID = "s"
    private const val PARAM_KEY = "k"

    fun receiverPageUrl(target: PairingTarget): String =
        "http://${target.authority}$RECEIVER_PAGE_PATH" +
                "#$PARAM_KEY=${PairingBase64.encode(target.key)}&$PARAM_SID=${target.sid}"

    fun appDeepLink(target: PairingTarget): String =
        "$APP_SCHEME://$APP_HOST?$PARAM_HOST=${target.authority}&$PARAM_SID=${target.sid}" +
                "#$PARAM_KEY=${PairingBase64.encode(target.key)}"

    fun parse(link: String, allowLoopback: Boolean = false): PairingTarget? {
        val trimmed = link.trim()
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd <= 0) return null
        val scheme = trimmed.substring(0, schemeEnd).lowercase()
        val afterScheme = trimmed.substring(schemeEnd + 3)
        val fragment = afterScheme.substringAfter('#', missingDelimiterValue = "")
        val beforeFragment = afterScheme.substringBefore('#')
        val query = beforeFragment.substringAfter('?', missingDelimiterValue = "")
        val authorityAndPath = beforeFragment.substringBefore('?')
        val authority = authorityAndPath.substringBefore('/')
        val path = authorityAndPath.substring(authority.length)
        val fragmentParams = parseParams(fragment) ?: return null
        val queryParams = parseParams(query) ?: return null
        return when (scheme) {
            APP_SCHEME -> {
                if (!authority.equals(APP_HOST, ignoreCase = true)) return null
                if (path.isNotEmpty() && path != "/") return null
                buildTarget(
                    hostAndPort = queryParams[PARAM_HOST],
                    sid = queryParams[PARAM_SID],
                    key = fragmentParams[PARAM_KEY] ?: queryParams[PARAM_KEY],
                    allowLoopback = allowLoopback,
                )
            }

            "http" -> {
                if (path != RECEIVER_PAGE_PATH) return null
                buildTarget(
                    hostAndPort = authority,
                    sid = fragmentParams[PARAM_SID],
                    key = fragmentParams[PARAM_KEY],
                    allowLoopback = allowLoopback,
                )
            }

            else -> null
        }
    }

    private fun buildTarget(
        hostAndPort: String?,
        sid: String?,
        key: String?,
        allowLoopback: Boolean,
    ): PairingTarget? {
        if (hostAndPort == null || sid == null || key == null) return null
        val separator = hostAndPort.lastIndexOf(':')
        if (separator <= 0) return null
        val host = hostAndPort.substring(0, separator)
        if (!isPrivateIpv4(host, allowLoopback)) return null
        val port = parsePort(hostAndPort.substring(separator + 1)) ?: return null
        if (PairingBase64.decodeOrNull(sid)?.size != PairingEnvelope.SID_SIZE) return null
        val keyBytes = PairingBase64.decodeOrNull(key)
            ?.takeIf { it.size == PairingEnvelope.KEY_SIZE }
            ?: return null
        return PairingTarget(host = host, port = port, sid = sid, key = keyBytes)
    }

    private fun parsePort(text: String): Int? {
        if (text.isEmpty() || text.length > 5 || !text.all { it in '0'..'9' }) return null
        return text.toInt().takeIf { it in 1..65535 }
    }

    private fun parseParams(text: String): Map<String, String>? {
        if (text.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        for (part in text.split('&')) {
            if (part.isEmpty()) continue
            val name = part.substringBefore('=')
            val value = part.substringAfter('=', missingDelimiterValue = "")
            val decoded = runCatching { value.decodeURLQueryComponent() }.getOrNull() ?: return null
            if (result.put(name, decoded) != null) return null
        }
        return result
    }
}
