package ru.slartus.boostbuddy.utils

internal object VersionsComparer {
    fun String.greaterThan(version2: String): Boolean = compareVersions(this, version2) == 1

    /**
     *  Если первая версия больше второй, то мы возвращаем 1.
     *  Если вторая версия больше первой, то мы возвращаем -1
     *  0 - равны
     */
    fun compareVersions(version1: String, version2: String): Int {
        if (version1.isEmpty()) {
            return if (version2.isEmpty()) 0 else -1
        }
        if (version2.isEmpty()) {
            return 1
        }
        val v1 = version1.split(".")
        val v2 = version2.split(".")
        val len = maxOf(v1.size, v2.size)

        for (i in 0 until len) {
            val num1 = if (i < v1.size) v1[i].takeWhile { it.isDigit() }.toInt() else 0
            val num2 = if (i < v2.size) v2[i].takeWhile { it.isDigit() }.toInt() else 0
            if (num1 > num2) return 1
            if (num1 < num2) return -1
        }
        return 0
    }
}