package ru.slartus.boostbuddy.data.log

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

fun debugLogBuild() {
    Napier.base(DebugAntilog())
}