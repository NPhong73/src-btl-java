@echo off
title Planet3D - 3D Planet Simulation
echo ==============================================
echo   Planet3D - Ung dung Mo phong Hanh tinh 3D
echo   Version 1.0 ^| Java 17 + JOGL + SQLite
echo ==============================================
echo.

REM Kiem tra Java
java -version 2>nul
if errorlevel 1 (
    echo [LOI] Java chua duoc cai dat!
    echo Vui long cai dat Java JDK 17 tu: https://adoptium.net/
    pause
    exit /b 1
)

echo [OK] Java da san sang
echo [INFO] Dang khoi dong ung dung...
echo.

java -Xmx1g ^
  -Dsun.java2d.noddraw=true ^
  -Dsun.java2d.d3d=false ^
  -Dsun.awt.noerasebackground=true ^
  -jar "%~dp0target\planet3d-jar-with-dependencies.jar"

if errorlevel 1 (
    echo.
    echo [LOI] Ung dung bi loi! Ma loi: %errorlevel%
    echo Kiem tra:
    echo  1. File JAR co ton tai trong thu muc target\ khong
    echo  2. Driver GPU co ho tro OpenGL 2.0+ khong
    echo  3. RAM du de chay ung dung khong
    pause
)
