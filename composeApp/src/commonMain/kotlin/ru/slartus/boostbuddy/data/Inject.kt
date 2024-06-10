package ru.slartus.boostbuddy.data

import org.kodein.di.DI
import org.kodein.di.DirectDI
import org.kodein.di.LazyDelegate
import org.kodein.di.conf.ConfigurableDI
import org.kodein.di.direct
import org.kodein.di.instance

object Inject {
    val di: DirectDI get() = _di.direct
    val diLazy: DI get() = _di.direct.lazy

    @Suppress("VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL")
    private lateinit var _di: ConfigurableDI

    fun createDependenciesTree(init: DI.MainBuilder.() -> Unit) {
        _di = ConfigurableDI(true).addConfig(init)
    }

    inline fun <reified T> instance(): T {
        return di.instance()
    }

    inline fun <reified T> lazy(tag: Any? = null): LazyDelegate<T> {
        return diLazy.instance(tag)
    }
}