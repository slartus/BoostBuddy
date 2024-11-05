package ru.slartus.boostbuddy.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

val monthsMap: Map<Int, String> = mapOf(
    1 to "янв",
    2 to "фев",
    3 to "мар",
    4 to "апр",
    5 to "май",
    6 to "июн",
    7 to "июл",
    8 to "авг",
    9 to "сен",
    10 to "окт",
    11 to "ноя",
    12 to "дек",
)

fun dateTimeFromUnix(unixTime: Long): LocalDateTime =
    Instant.fromEpochMilliseconds(unixTime * 1000)
        .toLocalDateTime(TimeZone.currentSystemDefault())

fun LocalDateTime.toHumanString(): String = buildString {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    append("$dayOfMonth ${monthsMap[monthNumber]}")
    if (now.year != year)
        append(" $year")
    append(" в ${hour}:${minute.toString().padStart(2, '0')}")
}

fun LocalDate.toHumanString(): String = buildString {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    append("$dayOfMonth ${monthsMap[monthNumber]}")
    if (now.year != year)
        append(" $year")
}