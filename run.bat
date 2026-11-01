@echo off
setlocal
title AttendX - Smart Attendance Management System

echo ========================================================
echo     AttendX - Smart Attendance Management System
echo     Smarter Attendance. Better Management.
echo ========================================================
echo.

set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if exist "%~dp0target\attendx-1.0.0.jar" (
    echo [INFO] Launching AttendX standalone package...
    "%JAVA_HOME%\bin\java.exe" -jar "%~dp0target\attendx-1.0.0.jar"
    goto :end
)

if exist "%~dp0maven\bin\mvn.cmd" (
    echo [INFO] Launching with included Maven distribution...
    "%~dp0maven\bin\mvn.cmd" javafx:run
    goto :end
)

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [INFO] System Apache Maven detected...
    mvn javafx:run
    goto :end
)

echo [ERROR] Neither executable package nor Maven was found.
pause

:end
endlocal
