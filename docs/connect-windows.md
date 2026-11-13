# Как подключиться к приставке (Windows)

Ваша приставка: **192.168.0.8** (MAC `ec:fa:5c:3b:d5:b3`), компьютер:
`192.168.0.15`. Оба в одной сети — это единственное обязательное условие.

---

## Шаг 1. Включите отладку на приставке

1. Возьмите пульт, откройте **Настройки**.
2. **Настройки → Об устройстве (Device Preferences → About)**.
3. Найдите **Номер сборки (Build)** и нажмите на него **7 раз** подряд.
   Появится сообщение «Вы стали разработчиком» / «You are now a developer».
4. Вернитесь назад: **Настройки → Настройки устройства →
   Для разработчиков (Developer options)**.
5. Включите:
   - **Отладка по USB (USB debugging)** — обязательно;
   - **Отладка по сети / ADB по сети (Network debugging)** — если пункт есть,
     включите. На Android 9 у Mi Box его часто нет, тогда см. шаг 4б.

Ничего страшного, что «USB», а мы по сети: этот пункт разрешает ADB в принципе.

## Шаг 2. Поставьте ADB на компьютер

ADB — это маленькая консольная программа, через неё компьютер ставит
приложения на приставку.

### Самый простой путь: две команды в PowerShell

Откройте PowerShell (Win + R → `powershell` → Enter) и выполните:

```powershell
$ProgressPreference = 'SilentlyContinue'
Invoke-WebRequest -Uri "https://dl.google.com/android/repository/platform-tools-latest-windows.zip" -OutFile "$env:USERPROFILE\Downloads\platform-tools.zip"
Expand-Archive -Path "$env:USERPROFILE\Downloads\platform-tools.zip" -DestinationPath "$env:USERPROFILE\Downloads" -Force
cd "$env:USERPROFILE\Downloads\platform-tools"
.\adb.exe version
```

Последняя команда должна вывести версию, например
`Android Debug Bridge version 1.0.41`. Дальше все команды `adb` выполняются
из этой папки (`$env:USERPROFILE\Downloads\platform-tools`).

> Если `Invoke-WebRequest` ругается на сертификат или долго висит — скачайте
> вручную: <https://developer.android.com/tools/releases/platform-tools>
> (кнопка «Download SDK Platform-Tools for Windows»), распакуйте архив,
> и перейдите в папку командой `cd`.

### Ручной путь

1. Скачайте **SDK Platform-Tools для Windows**:
   <https://developer.android.com/tools/releases/platform-tools>
2. Распакуйте архив, например в `C:\platform-tools` — внутри должен лежать
   `adb.exe`.
3. Откройте PowerShell в этой папке (в проводнике: Shift + правый клик по
   папке → «Открыть окно PowerShell здесь»).
4. Проверьте: `.\adb.exe version`.

> Ошибка `Имя ".\adb.exe" не распознано как имя командлета` означает ровно
> одно: вы **не в папке с adb.exe** (частая ситуация — окно PowerShell открыто
> в `C:\WINDOWS\system32`). Проверьте `Get-Location` и `Get-ChildItem adb.exe`,
> затем `cd` в папку `platform-tools`.

> Если Windows ругается на «не удалось проверить издателя» — это нормально
> для platform-tools, файлы подписаны Google.

## Шаг 3. Подключитесь

```powershell
.\adb.exe connect 192.168.0.8:5555
```

Ожидаемый ответ: `connected to 192.168.0.8:5555`.

Теперь на **экране телевизора** появится запрос:
«Разрешить отладку по USB? / Allow USB debugging?» с отпечатком ключа RSA.

✅ Поставьте галочку **«Всегда разрешать с этого компьютера»**
   и нажмите **ОК** (пультом).

Проверьте, что устройство видно:

```powershell
.\adb.exe devices
```

Должно быть:

```
List of devices attached
192.168.0.8:5555    device
```

`device` — всё хорошо. `unauthorized` — не нажали ОК на ТВ (шаг 3 повторите).
`offline` — выполните `.\adb.exe disconnect` и `connect` заново.

## Шаг 4а. Убедитесь, что это ваша приставка

```powershell
.\adb.exe -s 192.168.0.8:5555 shell getprop ro.product.model
.\adb.exe -s 192.168.0.8:5555 shell getprop ro.build.version.release
```

Ожидаемо что-то вроде `Mi Box S` / `MiTV-...` / `MDZ-22-AB` и `9`.

## Шаг 4б. Если `adb connect` не работает (нет «Отладки по сети»)

На Android 9 у части прошивок Mi Box ADB по сети по умолчанию выключен,
и включить его можно только «через кабель»:

1. Возьмите кабель **USB A–A** (male-to-male) — обычный USB-кабель для
   принтера/жёсткого диска **не подойдёт**.
2. Подключите приставку к компьютеру этим кабелем.
3. В PowerShell:

   ```powershell
   .\adb.exe devices              # приставка должна появиться
   .\adb.exe tcpip 5555           # переключаем ADB в сетевой режим
   ```

4. Кабель можно отключить, дальше — по сети:

   ```powershell
   .\adb.exe connect 192.168.0.8:5555
   ```

> После перезагрузки приставки сетевой ADB отключается — повторите
> `adb tcpip 5555` по кабелю. Для установки одного приложения это неважно.

Альтернатива кабелю: если в меню разработчика есть пункт
**«Отладка по сети»** — включите его, и `connect` заработает сразу.

## Шаг 4в. Если подключение уже работает

Отлично — дальше нужен только файл `app-debug.apk`:

- **Собранный APK у вас есть?** Переходите к шагу 5.
- **APK пока нет** — его нужно собрать, 10 минут в облаке GitHub (сборка идёт
  на серверах GitHub, ничего устанавливать не нужно):
  1. Откройте репозиторий на GitHub → **Add file → Create new file**.
  2. В поле имени введите `.github/workflows/build.yml` (слэши создадут папки).
  3. Скопируйте туда содержимое файла `ci/github-actions-build.yml`
     из репозитория и нажмите **Commit changes**.
  4. Через ~5 минут: вкладка **Actions → Build APK → последний запуск →
     Artifacts → tv-launcher-debug**. Скачается zip, внутри `app-debug.apk`.
  5. Положите файл, например, в `%USERPROFILE%\Downloads\app-debug.apk`.

## Шаг 5. Дальше можно ставить лаунчер

```powershell
.\adb.exe install -r app-debug.apk
```

или сразу всё вместе (установка + домашний экран + отключение PatchWall):

```powershell
# из папки с репозиторием, в Git Bash
./scripts/xiaomi-setup.sh 192.168.0.8
```

---

## Проверка «на пальцах» без ADB

Если ADB не поднимается совсем, есть путь без компьютера:
скопируйте `app-debug.apk` на флешку (FAT32) → вставьте в приставку →
откройте файловым менеджером (например «X-plore» или «File Commander» из
Play Маркета) → установите. Но переключить кнопку Home с PatchWall без ADB
на Mi Box нельзя.

---

## Частые ошибки при копировании команд

| Что вышло | Почему | Как исправить |
|---|---|---|
| `Имя ".\adb.exe" не распознано` | вы не в папке с `adb.exe` (обычно `C:\WINDOWS\system32`) | `cd "$env:USERPROFILE\Downloads\platform-tools"` |
| `.db.exe version\` | лишний `\` в конце строки | печатайте команду без него: `.db.exe version` |
| две команды слиплись в одну (`devices.db.exe connect ...`) | не нажали Enter после первой | выполняйте по одной строке |
| `adb is not recognized` при вызове просто `adb` | папка не в PATH | используйте `.\adb.exe` или добавьте папку в PATH |
