@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
title Fusion Code - FRC Web Simulator

where javac >nul 2>nul
if errorlevel 1 (
  echo.
  echo  [TR] Java ^(JDK^) bulunamadi. JDK 21 kurun: https://adoptium.net  ^(Temurin 21^)
  echo       Kurduktan sonra bu dosyaya tekrar cift tiklayin.
  echo  [EN] Java ^(JDK^) not found. Install JDK 21 from https://adoptium.net then run this again.
  echo.
  pause
  exit /b 1
)

if not exist build\frcsim-core.jar (
  echo  [TR] build klasoru eksik. Once "bash build.sh" calistirin veya hazir zip'i kullanin.
  echo  [EN] build folder missing. Use the prebuilt zip or run build.sh first.
  pause
  exit /b 1
)

set PORT=8080
if not "%~1"=="" set PORT=%~1

echo.
echo  Fusion Code basliyor / starting:  http://127.0.0.1:%PORT%
echo  Kapatmak icin bu pencereyi kapatin / close this window to stop.
echo.
start "" cmd /c "timeout /t 3 >nul & start http://127.0.0.1:%PORT%"
java -cp build\frcsim-core.jar;build\server-classes frcsim.server.Server --root . --port %PORT%
echo.
pause
