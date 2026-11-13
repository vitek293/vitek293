# Подключение к приставке по ADB — Windows PowerShell
#
# Использование (из папки с adb.exe, либо укажите путь к adb):
#   powershell -ExecutionPolicy Bypass -File scripts\connect.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\connect.ps1 -Ip 192.168.0.8
#
param(
    [string]$Ip = "192.168.0.8",
    [int]$Port = 5555,
    [string]$Adb = "adb"
)

$ErrorActionPreference = "Stop"
function Info($m) { Write-Host "> $m" -ForegroundColor Cyan }
function Ok($m)   { Write-Host "OK $m" -ForegroundColor Green }
function Warn($m) { Write-Host "! $m" -ForegroundColor Yellow }
function Fail($m) { Write-Host "X $m" -ForegroundColor Red }

# --- adb на месте? ------------------------------------------------------------
try {
    $ver = & $Adb version 2>&1 | Select-Object -First 1
    Ok "ADB: $ver"
} catch {
    Fail "adb.exe не найден."
    Warn "Скачайте platform-tools: https://developer.android.com/tools/releases/platform-tools"
    Warn "Распакуйте в C:\platform-tools и запустите: .\scripts\connect.ps1 -Adb C:\platform-tools\adb.exe"
    exit 1
}

# --- проверка доступности приставки в сети ------------------------------------
Info "Проверяю $Ip ..."
if (-not (Test-Connection -ComputerName $Ip -Count 2 -Quiet)) {
    Fail "$Ip не отвечает на ping."
    Warn "Приставка выключена/в другой сети/сменила IP после перезагрузки роутера."
    Warn "Проверьте IP на ТВ: Настройки -> Сеть и интернет -> ваша сеть -> IP-адрес."
    exit 1
}
Ok "Приставка в сети"

# --- подключение --------------------------------------------------------------
Info "Подключаюсь к ${Ip}:${Port} ..."
& $Adb connect "${Ip}:${Port}" | Out-Null
Start-Sleep -Seconds 2

$serial = "${Ip}:${Port}"
$devices = & $Adb devices
if ($devices -match [regex]::Escape($serial) + "\s+device") {
    Ok "Подключено: $serial"
} elseif ($devices -match [regex]::Escape($serial) + "\s+unauthorized") {
    Warn "Устройство видно, но не авторизовано."
    Warn "На экране ТЕЛЕВИЗОРА нажмите ОК в запросе «Разрешить отладку по USB?»"
    Warn "Если запроса нет — отключите и включите «Отладка по USB» на приставке."
    exit 2
} else {
    Fail "Не удалось подключиться."
    Warn "1) Включена ли «Отладка по USB» в меню «Для разработчиков»?"
    Warn "2) Если пункта «Отладка по сети» нет — один раз подключите кабель USB A-A"
    Warn "   и выполните: adb tcpip 5555"
    exit 3
}

# --- информация об устройстве --------------------------------------------------
$model   = (& $Adb -s $serial shell getprop ro.product.model) -replace "`r", ""
$android = (& $Adb -s $serial shell getprop ro.build.version.release) -replace "`r", ""
$sdk     = (& $Adb -s $serial shell getprop ro.build.version.sdk) -replace "`r", ""
Ok "Устройство: $model | Android $android (SDK $sdk)"

if ($sdk -and [int]$sdk -lt 21) {
    Fail "Android слишком старый (SDK $sdk < 21) — лаунчер не установится."
    exit 4
}

Write-Host ""
Ok "Готово. Дальше можно ставить лаунчер:"
Write-Host "    .\adb.exe install -r app-debug.apk" -ForegroundColor Gray
Write-Host "    .\adb.exe shell cmd package set-home-activity com.vitek.tvlauncher/.MainActivity" -ForegroundColor Gray
