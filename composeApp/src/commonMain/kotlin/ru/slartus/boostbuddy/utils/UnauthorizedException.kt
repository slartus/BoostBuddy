package ru.slartus.boostbuddy.utils

class UnauthorizedException : Exception()

inline fun unauthorizedError(): Nothing = throw UnauthorizedException()