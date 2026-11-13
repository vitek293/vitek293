# Один запуск: публикует workflow сборки APK и запускает его.
# Использование:  powershell -ExecutionPolicy Bypass -File setup-workflow.ps1 -Token ghp_xxxxx
# Токен: https://github.com/settings/tokens/new  ->  галочка "workflow" (и repo)
param(
    [Parameter(Mandatory = $true)][string]$Token,
    [string]$Owner = "vitek293",
    [string]$Repo = "vitek293"
)

$ErrorActionPreference = "Stop"
$Api = "https://api.github.com/repos/$Owner/$Repo/contents/.github/workflows/build.yml"
$Headers = @{ Authorization = "token $Token"; "User-Agent" = "tv-launcher-setup" }

$yaml = @'
name: Build APK

on:
  push:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v4
      - name: Build debug APK
        run: |
          chmod +x ./gradlew
          ./gradlew assembleDebug --stacktrace --no-daemon
      - uses: actions/upload-artifact@v4
        with:
          name: tv-launcher-debug
          path: app/build/outputs/apk/debug/app-debug.apk
          if-no-files-found: error
'@

$body = @{
    message = "Add APK build workflow"
    content = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($yaml))
    branch  = "arena/b9c625a2-vitek293"
} | ConvertTo-Json

Write-Host "> Публикую workflow ..." -ForegroundColor Cyan
try {
    $r = Invoke-RestMethod -Uri $Api -Method Put -Headers $Headers -Body $body -ContentType "application/json"
    Write-Host "OK файл создан: $($r.content.html_url)" -ForegroundColor Green
} catch {
    Write-Host "X Не удалось создать файл: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  Чаще всего: у токена нет права 'workflow' (classic) или Actions: Read and write (fine-grained)." -ForegroundColor Yellow
    exit 1
}

Start-Sleep -Seconds 3
Write-Host "> Запускаю сборку ..." -ForegroundColor Cyan
$run = @{ ref = "arena/b9c625a2-vitek293" } | ConvertTo-Json
try {
    Invoke-RestMethod -Uri "https://api.github.com/repos/$Owner/$Repo/actions/workflows/build.yml/dispatches" `
        -Method Post -Headers $Headers -Body $run -ContentType "application/json" | Out-Null
    Write-Host "OK сборка запущена" -ForegroundColor Green
} catch {
    Write-Host "! Автозапуск не сработал (запустите вручную на вкладке Actions): $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Через ~4-6 минут: https://github.com/$Owner/$Repo/actions" -ForegroundColor White
Write-Host "Открыть запуск 'Build APK' -> Artifacts -> tv-launcher-debug.zip -> внутри app-debug.apk" -ForegroundColor Gray
