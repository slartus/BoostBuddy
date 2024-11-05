package ru.slartus.boostbuddy.components.main

enum class MainViewNavigationItem {
    Feed, Subscribes
}

val MainViewNavigationItem.title: String get() = when(this){
    MainViewNavigationItem.Feed -> "Лента"
    MainViewNavigationItem.Subscribes -> "Подписки"
}