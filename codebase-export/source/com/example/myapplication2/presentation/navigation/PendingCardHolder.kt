package com.example.myapplication2.presentation.navigation

import com.example.myapplication2.core.model.DashboardCard

/**
 * Зберігає картку для миттєвого відображення при відкритті з пошуку,
 * щоб уникнути затримки завантаження з БД.
 */
object PendingCardHolder {
    var card: DashboardCard? = null
}
