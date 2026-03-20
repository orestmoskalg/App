# Desktop `files (2)` / `cursor-package`

У каталозі на робочому столі:

`C:\Users\Orest\Desktop\files (2)\cursor-package\`

лежить **референсний** Android-пакет V2 (`com.regulation.assistant.*`) плюс `CURSOR_PROMPT.md`.

У **цьому** репозиторії код зібраний у **`com.example.myapplication2`** (див. корінь `docs/V2.md`). Файли з `cursor-package` не копіюються шаром 1:1, щоб не дублювати ViewModel і Room.

Що з референсу **збережено тут як артефакт**:

- `docs/imports/CURSOR_PROMPT_V2_CURSOR_PACKAGE.md` — копія `cursor-package/CURSOR_PROMPT.md` (і кореневого `files (2)/CURSOR_PROMPT.md` — усі три файли збігаються).

Повний список файлів на диску та статус порівняння з репо: `docs/imports/FILES2_FULL_AUDIT.md`.

Оновлювати логіку застосунку слід у модулі `app/`, а `cursor-package` на диску використовувати для порівняння текстів (онбординг, глосарій, standing rules).
