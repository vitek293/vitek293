#!/usr/bin/env bash
#
# Установка TV Launcher на Xiaomi Mi Box / Mi TV Stick (Android 9)
# и переключение домашнего экрана с PatchWall на лаунчер.
#
# Использование:
#   ./scripts/xiaomi-setup.sh                 # найдёт приставку сам
#   ./scripts/xiaomi-setup.sh 192.168.0.8     # или укажите IP вручную
#   APK=path/to/app-debug.apk ./scripts/xiaomi-setup.sh
#
# Нужен adb (platform-tools) на компьютере и включённая отладка по USB
# в настройках приставки.

set -uo pipefail

IP="${1:-}"
PORT="5555"
PKG="com.vitek.tvlauncher"
HOME_ACTIVITY="$PKG/.MainActivity"
APK="${APK:-app/build/outputs/apk/debug/app-debug.apk}"

# Пакеты PatchWall / домашних экранов Xiaomi, которые нужно отключить,
# чтобы кнопка Home вела в наш лаунчер.
XIAOMI_HOME_PACKAGES=(
  "com.mitv.tvhome.atv"            # PatchWall, международные Mi Box / Mi Stick
  "com.mitv.tvhome"                # PatchWall, китайские прошивки
  "com.google.android.tvlauncher"  # штатный Android TV launcher
  "com.google.android.apps.tv.launcherx" # штатный Google TV launcher
)

c() { printf '\033[%sm%s\033[0m\n' "$1" "$2"; }
info() { c "1;36" "▸ $*"; }
ok()   { c "1;32" "✓ $*"; }
warn() { c "1;33" "! $*"; }
err()  { c "1;31" "✗ $*"; }

if ! command -v adb >/dev/null 2>&1; then
  err "adb не найден. Установите platform-tools: https://developer.android.com/tools/releases/platform-tools"
  exit 1
fi

# --- 1. Подключение -----------------------------------------------------------
connect() {
  local ip="$1"
  info "Подключаюсь к $ip:$PORT ..."
  adb connect "$ip:$PORT" >/dev/null 2>&1
  sleep 2
  if adb -s "$ip:$PORT" shell true >/dev/null 2>&1; then
    ok "Подключено: $ip:$PORT"
    return 0
  fi
  return 1
}

if [[ -z "$IP" ]]; then
  info "Ищу приставку в локальной сети..."
  SUBNET="$(ip route 2>/dev/null | awk '/default/ {print $3}' | head -1)"
  SUBNET="${SUBNET%.*}"
  [[ -z "$SUBNET" ]] && SUBNET="192.168.0"
  for i in $(seq 1 254); do
    (adb connect "$SUBNET.$i:$PORT" >/dev/null 2>&1) &
  done
  wait
  DEV="$(adb devices | awk -v p="$PORT" '$1 ~ ":"p"$" {print $1}' | head -1)"
  if [[ -n "$DEV" ]]; then
    IP="${DEV%:*}"; PORT="${DEV##*:}"
    ok "Найдено устройство: $DEV"
  else
    err "Приставка не найдена. Укажите IP вручную: ./scripts/xiaomi-setup.sh 192.168.0.8"
    exit 1
  fi
else
  connect "$IP" || {
    err "Не удалось подключиться к $IP:$PORT"
    warn "Проверьте: приставка и компьютер в одной сети; в настройках включена «Отладка по USB»;"
    warn "на экране ТВ нажат «ОК» в запросе на разрешение отладки."
    warn "Если пункта «Отладка по сети» нет — один раз подключите кабель USB A-A и выполните: adb tcpip 5555"
    exit 1
  }
fi

SERIAL="$IP:$PORT"
adb_dev() { adb -s "$SERIAL" "$@"; }

# --- 2. Проверка устройства ---------------------------------------------------
MODEL="$(adb_dev shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
ANDROID_VER="$(adb_dev shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')"
SDK="$(adb_dev shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')"
ok "Устройство: ${MODEL:-?} — Android ${ANDROID_VER:-?} (SDK ${SDK:-?})"

if [[ -n "${SDK:-}" && "$SDK" -lt 21 ]]; then
  err "Android слишком старый (SDK $SDK < 21), лаунчер не установится."
  exit 1
fi

# --- 3. Установка APK ---------------------------------------------------------
if [[ ! -f "$APK" ]]; then
  err "APK не найден: $APK"
  warn "Соберите его (./gradlew assembleDebug) или укажите путь: APK=/путь/app-debug.apk $0 $IP"
  exit 1
fi

info "Устанавливаю $APK ..."
if adb_dev install -r "$APK"; then
  ok "APK установлен"
else
  warn "Повторная попытка после удаления старой версии..."
  adb_dev uninstall "$PKG" >/dev/null 2>&1
  adb_dev install "$APK" || { err "Установка не удалась"; exit 1; }
fi

adb_dev shell pm enable --user 0 "$PKG" >/dev/null 2>&1

# --- 4. Назначаем домашним экраном -------------------------------------------
info "Назначаю $HOME_ACTIVITY домашним экраном..."
if adb_dev shell cmd package set-home-activity "$HOME_ACTIVITY" >/dev/null 2>&1; then
  ok "Домашний экран назначен"
else
  warn "cmd package set-home-activity не сработал — нажмите Home на пульте и выберите TV Launcher"
fi

# --- 5. Отключаем PatchWall и старые лаунчеры ---------------------------------
# Отключаем только те пакеты, которые реально установлены на устройстве.
DISABLED=()
for p in "${XIAOMI_HOME_PACKAGES[@]}"; do
  if adb_dev shell pm list packages "$p" 2>/dev/null | tr -d '\r' | grep -qx "package:$p"; then
    # Не отключаем сами себя
    [[ "$p" == "$PKG" ]] && continue
    if adb_dev shell pm disable-user --user 0 "$p" >/dev/null 2>&1; then
      DISABLED+=("$p")
      ok "Отключён: $p"
    else
      warn "Не удалось отключить: $p"
    fi
  fi
done

if [[ ${#DISABLED[@]} -eq 0 ]]; then
  warn "Пакеты PatchWall не найдены — возможно, платформа не Xiaomi или уже настроена."
fi

# --- 6. Итог ------------------------------------------------------------------
echo
ok "Готово. Нажмите Home на пульте — должен открыться TV Launcher."
echo
if [[ ${#DISABLED[@]} -gt 0 ]]; then
  warn "Откат (вернуть PatchWall и штатный лаунчер):"
  for p in "${DISABLED[@]}"; do
    echo "    adb -s $SERIAL shell pm enable --user 0 $p"
  done
  echo "    adb -s $SERIAL uninstall $PKG"
fi
