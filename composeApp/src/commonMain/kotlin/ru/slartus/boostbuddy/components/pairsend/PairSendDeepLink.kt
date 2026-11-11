package ru.slartus.boostbuddy.components.pairsend

import ru.slartus.boostbuddy.data.pairing.PairingLink
import ru.slartus.boostbuddy.data.pairing.PairingTarget

internal sealed interface PairSendDeepLinkDecision {
    data class Open(val target: PairingTarget) : PairSendDeepLinkDecision
    data object InvalidLink : PairSendDeepLinkDecision
    data object NotLoggedIn : PairSendDeepLinkDecision
}

internal fun resolvePairSendDeepLink(
    url: String,
    allowLoopback: Boolean,
    isLoggedIn: Boolean,
): PairSendDeepLinkDecision {
    val target = PairingLink.parse(url, allowLoopback = allowLoopback)
        ?: return PairSendDeepLinkDecision.InvalidLink
    if (!isLoggedIn) return PairSendDeepLinkDecision.NotLoggedIn
    return PairSendDeepLinkDecision.Open(target)
}
