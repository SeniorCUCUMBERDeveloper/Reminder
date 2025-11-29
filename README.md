# Reminder — периодические напоминания (WorkManager + Compose)

Учебное Android‑приложение на Kotlin/Jetpack Compose, которое показывает
периодические напоминания каждые **N минут** с использованием **WorkManager**.

Приложение реализует требования домашнего задания *«Периодические напоминания — средний уровень»*:

- Поле ввода интервала (минуты, **не меньше 15**)
- Чекбоксы:
  - **Только при зарядке**
  - **Только при полной батарее**
- Кнопки **«Старт»** и **«Стоп»**
- `PeriodicWorkRequest` с `Constraints` на основе выбранных чекбоксов
- Управление через теги: `addTag("periodic")` и `cancelAllWorkByTag("periodic")`
- В уведомлении отображается **время следующего запуска**
- В UI отображается текущий статус `WorkInfo.State`
- Отдельная кнопка **«Проверить сейчас»** — одноразовый `OneTimeWorkRequest` с теми же условиями

---

## 1. Технологический стек

- **Kotlin**, Kotlin DSL
- **Jetpack Compose** + Material 3
- **WorkManager** (`work-runtime-ktx`)
- **AndroidX Lifecycle** (`AndroidViewModel`, `viewModelCompose`)
- Target / compile SDK: **36**
- Min SDK: **26**
- Java / JVM: **21**
- Разрешения:
  - `POST_NOTIFICATIONS` (Android 13+)

---

## 2. Структура проекта

Пакеты внутри `com.example.reminder`:

- `ui`
  - `ReminderScreen.kt` — основной Compose‑экран с UI и логикой запуска/остановки воркеров
  - `theme/` — тема приложения (генерируется Android Studio)
- `viewmodel`
  - `ReminderViewModel.kt` — предоставляет Application Context для Compose‑экрана
- `worker`
  - `ReminderWorker.kt` — `Worker`, который показывает системное уведомление
  - `NotificationReceiver.kt` — `BroadcastReceiver`, обрабатывающий действие «Стоп» из уведомления
- Корень пакета:
  - `MainActivity.kt` — точка входа, запрашивает разрешение на уведомления и показывает `ReminderScreen`

---

## 3. Gradle‑конфигурация (кратко)

В `app/build.gradle.kts`:

- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`
- `compose = true`
- `sourceCompatibility` / `targetCompatibility` = `JavaVersion.VERSION_21`
- `kotlinOptions.jvmTarget = "21"`
- Основные зависимости:
  - `androidx.activity:activity-compose`
  - `androidx.compose.*` (UI, tooling, Material3, runtime-livedata)
  - `androidx.lifecycle:lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`
  - `androidx.work:work-runtime-ktx`
  - `appcompat`, `material`

---

## 4. Проверка условий через ADB (зарядка / уровень батареи)

Для тестирования логики чекбоксов можно использовать команды `adb`.

### 4.1. Эмуляция уровня батареи

```bash
adb shell dumpsys battery set level 10   # установить заряд на 10%
adb shell dumpsys battery set level 100  # установить заряд на 100%
```

### 4.2. Эмуляция подключения/отключения зарядки

```bash
adb shell dumpsys battery unplug   # эмуляция работы НЕ на зарядке
adb shell dumpsys battery set ac 1 # эмуляция зарядки от сети
```

Вернуть реальные значения после теста:

```bash
adb shell dumpsys battery reset
```

### Ожидаемое поведение

- Если отмечен чекбокс **«Только при зарядке»**, а `unplug` →
  - периодический и одноразовый воркер будут уходить в `Result.retry()`
  - уведомление не придёт до тех пор, пока устройство не будет «заряжаться»
- Если отмечен чекбокс **«Только при полной батарее»**, а уровень < 100% →
  - воркер тоже вернёт `Result.retry()`
  - после `set level 100` и выполнения триггера уведомление появится

---
