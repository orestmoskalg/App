# Інструкція для завантаження в Google Play Store

---

## ⚠️ Помилка "signed in debug mode"?

Якщо Play Console показує, що AAB підписаний у debug режимі — потрібно створити **release keystore** (див. розділ 2 нижче), після цього перезібрати AAB через `build_playstore.ps1`.

---

## 1. Підготовка AAB файлу

### Варіант A: Через скрипт (рекомендовано)
1. Запустіть `build_playstore.ps1` у папці проєкту
2. AAB та ця інструкція будуть скопійовані на робочий стіл у папку `PlayStore_MyApplication2`

### Варіант B: Через Android Studio
1. Build → Generate Signed Bundle / APK
2. Оберіть **Android App Bundle**
3. Створіть або виберіть keystore
4. Збережіть `app-release.aab`

---

## 2. Keystore для підпису (перший раз)

Якщо ви ще не маєте keystore:

1. У Android Studio: Build → Generate Signed Bundle → Create new
2. Або через командний рядок:
```
keytool -genkey -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

3. Створіть файл `keystore.properties` у корені проєкту:
```properties
storeFile=upload-keystore.jks
storePassword=ваш_пароль
keyAlias=upload
keyPassword=ваш_пароль_ключа
```

**Важливо:** Зберігайте keystore та паролі в безпечному місці! Без них ви не зможете оновлювати застосунок в Play Store.

> **Keystore вже створено.** Файл `upload-keystore.jks` у корені проєкту. Пароль: `MyApp2024` (збережіть для майбутніх оновлень!)

---

## 3. Завантаження в Play Console

1. Перейдіть на [play.google.com/console](https://play.google.com/console)
2. Створіть новий застосунок (якщо ще немає)
3. Заповніть обов'язкові дані:
   - **Заявка про конфіденційність** (Privacy Policy URL)
   - **Опис застосунку** (короткий та повний)
   - **Скріншоти** (мінімум 2, рекомендується 4-8)
   - **Іконка** 512x512 px
   - **Feature graphic** 1024x500 px

4. Розділ **Випуск** → **Створити новий випуск**
5. Завантажте файл **MyApplication2-release.aab**
6. Додайте примітки до випуску
7. Натисніть **Зберегти** та **Переглянути випуск**

---

## 4. Контент застосунку

Заповніть усі обов'язкові розділи:
- Рейтинг контенту
- Цілі та функції
- Новини (якщо потрібно)
- Цільова аудиторія

---

## 5. Після завантаження

- Заявка пройде модерацію (зазвичай 1–7 днів)
- Ви отримаєте лист на пошту з результатом
- Збережіть keystore та паролі для майбутніх оновлень

---

## Поточна версія застосунку
- versionCode: 1
- versionName: 1.0
- applicationId: com.orest.regulation
