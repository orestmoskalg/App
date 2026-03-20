package com.example.myapplication2.core.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Перетворює технічні помилки на зрозумілі для користувача повідомлення.
 */
object ErrorMessageHelper {

    fun userFriendlyMessage(throwable: Throwable?): String {
        if (throwable == null) return "Щось пішло не так. Спробуйте ще раз."
        return when (throwable) {
            is UnknownHostException -> "Немає підключення до інтернету. Перевірте мережу."
            is SocketTimeoutException -> "Час очікування вийшов. Перевірте інтернет і спробуйте знову."
            is IOException -> "Помилка мережі. Перевірте з'єднання."
            else -> {
                val msg = throwable.message.orEmpty()
                when {
                    msg.contains("401", ignoreCase = true) -> "Помилка авторизації. Перевірте налаштування API."
                    msg.contains("403", ignoreCase = true) -> "Доступ заборонено. Перевірте API ключ."
                    msg.contains("429", ignoreCase = true) -> "Забагато запитів. Зачекайте і спробуйте пізніше."
                    msg.contains("500", ignoreCase = true) ||
                    msg.contains("502", ignoreCase = true) ||
                    msg.contains("503", ignoreCase = true) -> "Сервер тимчасово недоступний. Спробуйте пізніше."
                    msg.isNotBlank() && msg.length < 120 -> msg
                    else -> "Щось пішло не так. Спробуйте ще раз."
                }
            }
        }
    }
}
