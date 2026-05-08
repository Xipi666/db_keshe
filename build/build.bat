@echo off
setlocal EnableExtensions

set "ROOT_DIR=%~dp0.."
for %%I in ("%ROOT_DIR%") do set "ROOT_DIR=%%~fI"

set "FRONT_DIR=%ROOT_DIR%\Front"
set "CORE_DIR=%ROOT_DIR%\Core"
set "STATIC_DIR=%CORE_DIR%\src\main\resources\static"
set "DB_SQL_DIR=%CORE_DIR%\src\main\resources\db"
set "DIST_DIR=%FRONT_DIR%\dist"
set "RELEASE_DIR=%ROOT_DIR%\build"
set "RELEASE_SQL_DIR=%RELEASE_DIR%\sql"
set "JAR_NAME=database-keshe-0.0.1-SNAPSHOT.jar"

echo ======================================================
echo Build PSM-Smart System
echo ======================================================

if not exist "%FRONT_DIR%\package.json" (
    echo [ERROR] Frontend project not found: %FRONT_DIR%
    goto :FAIL
)

if not exist "%CORE_DIR%\pom.xml" (
    echo [ERROR] Backend project not found: %CORE_DIR%
    goto :FAIL
)

where npm >nul 2>nul
if errorlevel 1 (
    echo [ERROR] npm was not found in PATH.
    goto :FAIL
)

echo.
echo [1/5] Building frontend...
pushd "%FRONT_DIR%" || goto :FAIL
if not exist "node_modules" (
    echo [INFO] node_modules not found. Running npm install...
    call npm install
    if errorlevel 1 (
        popd
        goto :FAIL
    )
)

call npm run build
if errorlevel 1 (
    popd
    goto :FAIL
)
popd

if not exist "%DIST_DIR%\index.html" (
    echo [ERROR] Frontend build output not found: %DIST_DIR%
    goto :FAIL
)

echo.
echo [2/5] Copying frontend files to Spring Boot static resources...
if exist "%STATIC_DIR%" (
    rmdir /s /q "%STATIC_DIR%"
    if errorlevel 1 goto :FAIL
)

mkdir "%STATIC_DIR%"
if errorlevel 1 goto :FAIL

xcopy "%DIST_DIR%\*" "%STATIC_DIR%\" /E /I /Y >nul
if errorlevel 1 goto :FAIL

echo.
echo [3/5] Packaging backend jar...
set "MAVEN_CMD=%CORE_DIR%\mvnw.cmd"

if not exist "%MAVEN_CMD%" (
    echo [ERROR] Maven command not found: %MAVEN_CMD%
    goto :FAIL
)

pushd "%CORE_DIR%" || goto :FAIL
call "%MAVEN_CMD%" clean package -DskipTests
if errorlevel 1 (
    popd
    goto :FAIL
)
popd

echo.
echo [4/5] Copying jar to release directory...
set "SOURCE_JAR=%CORE_DIR%\target\%JAR_NAME%"
set "TARGET_JAR=%RELEASE_DIR%\%JAR_NAME%"

if not exist "%SOURCE_JAR%" (
    echo [ERROR] Jar not found: %SOURCE_JAR%
    goto :FAIL
)

copy /Y "%SOURCE_JAR%" "%TARGET_JAR%" >nul
if errorlevel 1 goto :FAIL

echo.
echo [5/5] Copying database init SQL to release directory...
if not exist "%DB_SQL_DIR%" (
    echo [ERROR] Database init SQL directory not found: %DB_SQL_DIR%
    goto :FAIL
)

if exist "%RELEASE_SQL_DIR%" (
    rmdir /s /q "%RELEASE_SQL_DIR%"
    if errorlevel 1 goto :FAIL
)

mkdir "%RELEASE_SQL_DIR%"
if errorlevel 1 goto :FAIL

xcopy "%DB_SQL_DIR%\*.sql" "%RELEASE_SQL_DIR%\" /E /I /Y >nul
if errorlevel 1 goto :FAIL

echo.
echo [DONE] Build finished.
echo Jar: %TARGET_JAR%
echo SQL: %RELEASE_SQL_DIR%
exit /b 0

:FAIL
echo.
echo [FAIL] Build failed.
exit /b 1
