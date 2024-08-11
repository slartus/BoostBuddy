package ru.slartus.boostbuddy.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun dateTimeFromUnix(unixTime: Long): LocalDateTime =
    Instant.fromEpochMilliseconds(unixTime * 1000)
        .toLocalDateTime(TimeZone.currentSystemDefault())