# Установка TV Launcher на приставку Xiaomi (Windows PowerShell)
#
# Делает: установку APK -> назначение домашним экраном -> отключение PatchWall.
# Требует уже подключённый ADB (см. docs/connect-windows.md).
#
# Использование:
#   powershell -ExecutionPolicy Bypass -File scripts\setup-windows.ps1 -Apk "$env:USERPROFILE\Downloads\app-debug.apk"
#   powershell -ExecutionPolicy Bypass -File scripts\setup-windows.ps1 -Apk ... -Ip 192.168.0.8 -Adb C:\platform-tools\adb.exe
#
param(
    [Parameter(Mandatory = $true)][string]$Apk,
    [string]$Ip = "192.168.0.8",
    [int]$Port = 5555,
    [string]$Adb = "adb",
    [switch]$UninstallFirst
)

$ErrorActionPreference = "Stop"
function Info($m) { Write-Host "> $m" -ForegroundColor Cyan }
function Ok($m)   { Write-Host "OK $m" -ForegroundColor Green }
function Warn($m) { Write-Host "! $m" -ForegroundColor Yellow }
function Fail($m) { Write-Host "X $m" -ForegroundColor Red }

$Pkg      = "com.vitek.tvlauncher"
$HomeAct  = "$Pkg/.MainActivity"
$Serial   = "${Ip}:${Port}"

# Пакеты «домашних экранов», которые могут перехватывать кнопку Home.
$HomePackages = @(
    "com.mitv.tvhome.atv",                  # PatchWall, международные Mi Box / Mi Stick
    "com.mitv.tvhome",                      # PatchWall, китайские прошивки
    "com.google.android.tvlauncher",        # штатный Android TV launcher
    "com.google.android.apps.tv.launcherx"  # штатный Google TV launcher
)

if (-not (Test-Path $Apk)) { Fail "APK не найден: $Apk"; exit 1 }

# --- соединение ---------------------------------------------------------------
Info "Проверяю соединение с $Serial ..."
$devices = & $Adb devices
if ($devices -notmatch [regex]::Escape($Serial) + "\s+device") {
    Warn "Устройство не подключено, пробую adb connect ..."
    & $Adb connect $Serial | Out-Null
    Start-Sleep -Seconds 2
    $devices = & $Adb devices
}
if ($devices -notmatch [regex]::Escape($Serial) + "\s+device") {
    Fail "Нет подключения. Сначала выполните: .\adb.exe connect $Serial"
    exit 1
}
Ok "Подключено: $Serial"

$model = (& $Adb -s $Serial shell getprop ro.product.model) -replace "`r", ""
$android = (& $Adb -s $Serial shell getprop ro.build.version.release) -replace "`r", ""
$sdk = (& $Adb -s $Serial shell getprop ro.build.version.sdk) -replace "`r", ""
Ok "Устройство: $model | Android $android (SDK $sdk)"

# --- установка ----------------------------------------------------------------
if ($UninstallFirst) {
    Info "Удаляю предыдущую версию ..."
    & $Adb -s $Serial uninstall $Pkg | Out-Null
}

Info "Устанавливаю $Apk ..."
$out = & $Adb -s $Serial install -r $Apk 2>&1
if ($out -match "Success") {
    Ok "APK установлен"
} else {
    Warn "Первая попытка не удалась: $out"
    Info "Пробую с полным удалением старой версии ..."
    & $Adb -s $Serial uninstall $Pkg | Out-Null
    $out = & $Adb -s $Serial install $Apk 2>&1
    if ($out -match "Success") { Ok "APK установлен (после переустановки)" }
    else { Fail "Установка не удалась: $out"; exit 1 }
}

# --- домашний экран -----------------------------------------------------------
Info "Назначаю домашним экраном ..."
$out = & $Adb -s $Serial shell cmd package set-home-activity $HomeAct 2>&1
if ($LASTEXITCODE -eq 0 -and $out -notmatch "Error") {
    Ok "Домашний экран: $HomeAct"
} else {
    Warn "set-home-activity не сработал ($out)"
    Warn "Нажмите Home на пульте и выберите TV Launcher в списке."
}

# --- отключаем PatchWall и старые лаунчеры -----------------------------------
$disabled = @()
foreach ($p in $HomePackages) {
    if ($p -eq $Pkg) { continue }
    $listed = & $Adb -s $Serial shell pm list packages $p 2>$null
    if ($listed -match "package:$([regex]::Escape($p))") {
        $res = & $Adb -s $Serial shell pm disable-user --user 0 $p 2>&1
        if ($res -match "new state: disabled") {
            Ok "Отключён: $p"
            $disabled += $p
        } else {
            Warn "Не удалось отключить $p : $res"
        }
    }
}
if ($disabled.Count -eq 0) {
    Warn "Пакеты PatchWall не найдены — возможно, устройство не Xiaomi или уже настроено."
}

# --- итог ---------------------------------------------------------------------
Write-Host ""
Ok "Готово. Нажмите Home на пульте — должен открыться TV Launcher."
Write-Host ""
if ($disabled.Count -gt 0) {
    Warn "Откат (вернуть как было):"
    foreach ($p in $disabled) {
        Write-Host "    .\adb.exe -s $Serial shell pm enable --user 0 $p" -ForegroundColor Gray
    }
    Write-Host "    .\adb.exe -s $Serial uninstall $Pkg" -ForegroundColor Gray
}
