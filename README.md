# 👻 AyuMode for exteraGram

[![Build classes.dex](https://github.com/exterawKey/AyuMode/actions/workflows/build.yml/badge.svg)](https://github.com/exterawKey/AyuMode/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Channel](https://img.shields.io/badge/Telegram-@exterawKey-blue.svg)](https://t.me/exterawKey)

**AyuMode** — плагин для клиента **exteraGram**, переносящий ключевые функции **AyuGram** (Режим призрака, сохранение удалённых сообщений, обход скриншотов и защиты медиа).

Проект построен на гибридной архитектуре: высокопроизводительный **`classes.dex` (Java)** для быстрой работы с пакетами и базой + легковесный **Python-плагин** для интеграции с движком exteraGram.

---

## 🌟 Возможности

### 👻 1. Режим призрака (Ghost Mode)
* **Нечиталка сообщений:** чтение диалогов и каналов без отправки статуса прочтения собеседнику.
* **Скрытие статуса «печатает...»:** запрет отправки статусов набора текста, записи голосовых и отправки медиа.
* **Скрытие онлайна:** предотвращение отправки пакетов обновления статуса «в сети».
* **Невидимка в историях:** просмотр чужих Stories без появления в списке зрителей.
* **Невидимка в медиа:** прослушивание голосовых и просмотр видеосообщений (кружочков) без отметки.

### 🔓 2. Обход ограничений (Anti-Protection)
* **Скриншоты везде:** снятие системного флага `FLAG_SECURE` в секретных чатах и закрытых каналах.
* **Скачивание из защищённых каналов:** принудительное отображение кнопки сохранения медиа даже при запрете сохранения контента автором.

### 🗑 3. Шпионские функции (Spy Features)
* **Сохранение удалённых сообщений:** стёртые сообщения остаются в чате.
* **История изменений:** сохранение первоначального текста отредактированных сообщений.

---

## 📂 Структура проекта

```text
AyuMode/
├── .github/workflows/
│   └── build.yml               # Автосборка classes.dex в GitHub Actions + Releases
├── src/main/java/com/exterawkey/ayumode/
│   ├── AyuModeBridge.java      # Главная точка входа для Python
│   ├── AyuModeConfig.java      # Конфигурация и хранилище настроек
│   ├── GhostController.java    # Фильтрация сетевых пакетов MTProto
│   ├── BypassController.java   # Обход FLAG_SECURE и защиты контента
│   └── MessageSaver.java       # Сохранение удалённых и изменённых сообщений
└── plugin/
    ├── ayumode.py              # Исходный код Python-плагина
    └── ayumode.plugin          # Скомпилированный плагин для exteraGram
```

---

## 🚀 Установка

1. Скачайте `ayumode.plugin` из [Releases](https://github.com/exterawKey/AyuMode/releases).
2. Откройте файл в **exteraGram** и нажмите **Установить**.
3. Настройте нужные функции в меню **Настройки exteraGram** > **Плагины** > **AyuMode**.

---

## ⚙️ Сборка classes.dex

Сборка `classes.dex` автоматизирована через **GitHub Actions** при каждом коммите в ветку `main`.
Для ручной сборки через `d8`:

```bash
javac -cp "$ANDROID_HOME/platforms/android-34/android.jar" -d build/classes src/main/java/com/exterawkey/ayumode/*.java
d8 --min-api 21 --output build/ build/classes/com/exterawkey/ayumode/*.class
```

---

👤 **Автор:** [wKey](https://t.me/exterawKey)  
📢 **Канал с плагинами:** [@exterawKey](https://t.me/exterawKey)  
💡 **Лицензия:** [MIT](LICENSE)
