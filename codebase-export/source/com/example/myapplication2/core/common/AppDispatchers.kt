package com.example.myapplication2.core.common

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Singleton
class AppDispatchers @Inject constructor() {
    val io: CoroutineDispatcher = Dispatchers.IO
}
