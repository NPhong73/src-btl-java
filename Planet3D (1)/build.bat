@echo off
title Planet3D Builder
echo Dang build Planet3D...
set MVN="%~dp0mvn-bin\apache-maven-3.9.6\bin\mvn.cmd"
%MVN% clean package -DskipTests
if %errorlevel% == 0 (
    echo.
    echo [OK] Build thanh cong! File JAR: target\planet3d-jar-with-dependencies.jar
    echo Chay ung dung: run.bat
) else (
    echo [LOI] Build that bai! Kiem tra log tren.
)
pause
