package ru.slartus.boostbuddy.data.pairing

import ru.slartus.boostbuddy.data.pairing.web.PairingWebPages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PairingWebPagesTest {

    private val sources = PairingWebPages.assets.mapValues { it.value.body } +
            ("bookmarklet" to PairingWebPages.BOOKMARKLET_JS)

    @Test
    fun noUnresolvedKotlinInterpolation() {
        val leftover = Regex("""\$[A-Za-z_{]""")
        sources.forEach { (name, body) -> assertFalse(leftover.containsMatchIn(body), name) }
    }

    @Test
    fun interpolatedPathsAreResolved() {
        assertTrue(PairingWebPages.SCRIPT_S.contains("fetch('/s/' + params.s"))
        assertTrue(PairingWebPages.PAGE_P.contains("""<script src="/p.js"></script>"""))
        assertTrue(PairingWebPages.PAGE_S.contains("""<script src="/s.js"></script>"""))
        assertTrue(PairingWebPages.PAGE_P.contains("""href="/pair.css""""))
    }

    @Test
    fun receiverPageShowsDonorLogoutWarning() {
        assertTrue(
            PairingWebPages.PAGE_P.contains(
                "После переноса этот браузер выйдет из Boosty — войти в нём можно будет заново."
            )
        )
    }

    @Test
    fun scriptsAvoidUnsafeDomApis() {
        listOf("innerHTML", "outerHTML", "insertAdjacentHTML", "eval(", "document.write", "new Function")
            .forEach { api -> sources.forEach { (name, body) -> assertFalse(body.contains(api), "$name uses $api") } }
    }

    @Test
    fun receiverScriptEmbedsBookmarklet() {
        assertTrue(PairingWebPages.SCRIPT_P.startsWith("var BOOKMARKLET_SOURCE = \"(function () {"))
        assertEquals(5, PairingWebPages.assets.size)
    }
}
