@echo off
chcp 65001 >nul
echo Building Tactic...
if exist build (rmdir /s /q build)
call gradlew.bat clean build
if errorlevel 1 (
    echo.
    echo Build FAILED.
    pause
    exit /b 1
)
echo.
echo Build OK! Jar is in build\libs\Tactic.jar
echo.
echo IMPORTANT: delete old Tactic.jar from server plugins/ before copying!
pause
