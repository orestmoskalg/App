package com.example.myapplication2

import android.app.Application
import com.example.myapplication2.di.AppContainer

class RegulationApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
