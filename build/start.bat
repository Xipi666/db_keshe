@echo off
:: set coding to UTF-8 without BOM
chcp 65001 > nul
setlocal enabledelayedexpansion

:: 2. 定义 ANSI 颜色宏
for /F "tokens=1,2 delims=#" %%a in ('"prompt #$H#$E# & echo on & for %%b in (1) do rem"') do set "ESC=%%b"

set "RED=%ESC%[31m"
set "GREEN=%ESC%[32m"
set "YELLOW=%ESC%[33m"
set "CYAN=%ESC%[36m"
set "RESET=%ESC%[0m"

:: 3. 基础设置
title PSM-Smart Startup Tool
set ROOT_DIR=%~dp0..
set JAR_NAME=database-keshe-0.0.1-SNAPSHOT.jar
set TARGET_JAR=%~dp0%JAR_NAME%
set SOURCE_JAR=%ROOT_DIR%\Core\target\%JAR_NAME%

set BG_MODE=false
if "%1"=="-b" set BG_MODE=true

echo %CYAN%======================================================%RESET% 
echo    %CYAN%PSM-Smart 变电站监控系统 - 演示模式%RESET% 
echo %CYAN%======================================================%RESET% 

:: 4. 数据库检查 
echo [%CYAN%1/3%RESET%] Checking Environment...

set DB_MODE=none
where wsl > nul 2> nul
if !errorlevel! == 0 (
    wsl docker version > nul 2> nul
    if !errorlevel! == 0 (
        set DB_MODE=wsl-docker
        set "DOCKER_CMD=wsl docker"
        goto :USE_WSL_DOCKER
    )
)

where docker > nul 2> nul
if !errorlevel! == 0 (
    docker version > nul 2> nul
    if !errorlevel! == 0 (
        set DB_MODE=docker-desktop
        set "DOCKER_CMD=docker"
        goto :USE_DOCKER_DESKTOP
    )
)

echo [Status] %YELLOW%Docker not available, checking local Oracle service...%RESET%
set LOCAL_ORACLE_SERVICE=
for /f "tokens=2 delims=:" %%s in ('sc query state^= all ^| findstr /I "SERVICE_NAME" ^| findstr /I "OracleService"') do (
    if not defined LOCAL_ORACLE_SERVICE (
        set "TEMP_ORACLE_SERVICE=%%s"
        set "LOCAL_ORACLE_SERVICE=!TEMP_ORACLE_SERVICE: =!"
    )
)

if defined LOCAL_ORACLE_SERVICE (
    echo [Status] %GREEN%Found local Oracle service: !LOCAL_ORACLE_SERVICE!.%RESET%
    net session > nul 2> nul
    if !errorlevel! neq 0 (
        echo [Warn] %YELLOW%Not running as administrator. Starting local Oracle services or using / as sysdba may fail.%RESET%
    )
    sc query "!LOCAL_ORACLE_SERVICE!" | findstr /I "RUNNING" > nul 2> nul
    if !errorlevel! neq 0 (
        echo [Status] %YELLOW%Starting local Oracle service...%RESET%
        net start "!LOCAL_ORACLE_SERVICE!"
        if !errorlevel! neq 0 goto :DB_ERROR
    )

    set LOCAL_LISTENER_SERVICE=
    for /f "tokens=2 delims=:" %%s in ('sc query state^= all ^| findstr /I "SERVICE_NAME" ^| findstr /I "Listener"') do (
        if not defined LOCAL_LISTENER_SERVICE (
            set "TEMP_LISTENER_SERVICE=%%s"
            set "LOCAL_LISTENER_SERVICE=!TEMP_LISTENER_SERVICE: =!"
        )
    )
    if defined LOCAL_LISTENER_SERVICE (
        sc query "!LOCAL_LISTENER_SERVICE!" | findstr /I "RUNNING" > nul 2> nul
        if !errorlevel! neq 0 (
            echo [Status] %YELLOW%Starting local Oracle listener...%RESET%
            net start "!LOCAL_LISTENER_SERVICE!"
        )
    )
    call :PREPARE_LOCAL_ORACLE
    if !errorlevel! neq 0 goto :DB_ERROR
    goto :LOCAL_DB_READY
)

echo [Error] %RED%Neither WSL Docker nor local Oracle database was found.%RESET%
echo [Error] %RED%Please install/start WSL Docker, Docker Desktop, or Oracle Database locally.%RESET%
goto :EXIT_SCRIPT

:USE_DOCKER_DESKTOP
echo [Status] %GREEN%Docker Desktop detected, using Docker Oracle.%RESET%
goto :USE_DOCKER

:USE_WSL_DOCKER
echo [Status] %GREEN%WSL Docker detected, using Docker Oracle.%RESET%

:USE_DOCKER
pushd "%ROOT_DIR%"

for /f "tokens=*" %%i in ('!DOCKER_CMD! ps -q -f "name=oracle21c" -f "status=running"') do set RUNNING_ID=%%i
if defined RUNNING_ID (
    echo [Status] %GREEN%Container oracle21c is already running.%RESET%
    goto :DB_READY
)

for /f "tokens=*" %%i in ('!DOCKER_CMD! ps -aq -f "name=oracle21c"') do set EXIST_ID=%%i
if defined EXIST_ID (
    echo [Status] %YELLOW%Starting stopped container...%RESET%
    !DOCKER_CMD! start oracle21c
    goto :DB_READY
)

echo [Status] %RED%Container not found, creating...%RESET%
!DOCKER_CMD! compose up -d
if !errorlevel! neq 0 goto :DB_ERROR
goto :DB_READY

:DB_ERROR
if "!DB_MODE!"=="wsl-docker" popd
if "!DB_MODE!"=="docker-desktop" popd
echo [Error] %RED%Database failed to start or initialize.%RESET%
goto :EXIT_SCRIPT

:DB_READY
popd
:LOCAL_DB_READY
echo [Wait] %YELLOW%Initializing (20s)...%RESET%
timeout /t 20 /nobreak > nul

:: 5. JAR 准备
echo.
echo [%CYAN%2/3%RESET%] Preparing backend executable...
echo ------------------------------------------------------
if exist "%TARGET_JAR%" (
    echo [Status] %GREEN%Found %JAR_NAME%.%RESET%
) else (
    if exist "%SOURCE_JAR%" (
        copy "%SOURCE_JAR%" "%TARGET_JAR%" > nul
        echo [Done] %GREEN%Sync from source complete.%RESET%
    ) else (
        echo [Error] %RED%JAR not found! Run 'mvnw package'.%RESET%
        goto :EXIT_SCRIPT
    )
)

:: 5.5 清理 8080 端口 (防御性增强版)
echo [Wait] %YELLOW%Cleaning up port 8080...%RESET%

:: 先将 PID 存入一个临时变量，注意这里使用了 ! 延迟扩展
set "TARGET_PID="
for /f "tokens=5" %%a in ('netstat -aon ^| findstr /r /c:":8080 " ^| findstr LISTENING') do (
    set "TARGET_PID=%%a"
)

:: 只有在变量被定义（即找到了 PID）的情况下才执行 kill
if defined TARGET_PID (
    echo [Status] %GREEN%Found process !TARGET_PID! on 8080, terminating...%RESET%
    taskkill /F /PID !TARGET_PID! > nul 2> nul
) else (
    echo [Status] %CYAN%Port 8080 is clear.%RESET%
)

:: 6. 启动服务
echo [%CYAN%3/3%RESET%] Starting Service...
if "%BG_MODE%"=="true" (
    echo [Info] %YELLOW%Running in BACKGROUND mode.%RESET%
    start /b java -Dfile.encoding=UTF-8 -jar "%TARGET_JAR%" --spring.config.import=optional:file:"%ROOT_DIR%\.env"
) else (
    echo [Info] %CYAN%Running in FOREGROUND mode.%RESET%
    java -Dfile.encoding=UTF-8 -jar "%TARGET_JAR%" --spring.config.import=optional:file:"%ROOT_DIR%\.env"
)

:EXIT_SCRIPT
echo ------------------------------------------------------
echo %GREEN%[Finished] Script execution completed.%RESET%
:: TODO: close db server
pause
exit /b

:PREPARE_LOCAL_ORACLE
where sqlplus > nul 2> nul
if !errorlevel! neq 0 (
    echo [Error] %RED%Local Oracle found, but sqlplus was not found in PATH.%RESET%
    echo [Error] %RED%Cannot create or initialize local database automatically.%RESET%
    exit /b 1
)

set "ORACLE_PASSWORD=oracle"
set "DB_USERNAME=psm_app"
set "DB_PASSWORD=psm_app_123"
set "DB_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1"

if exist "%ROOT_DIR%\.env" (
    for /f "usebackq tokens=1,* delims==" %%a in ("%ROOT_DIR%\.env") do (
        if /I "%%a"=="ORACLE_PASSWORD" set "ORACLE_PASSWORD=%%b"
        if /I "%%a"=="DB_USERNAME" set "DB_USERNAME=%%b"
        if /I "%%a"=="DB_PASSWORD" set "DB_PASSWORD=%%b"
        if /I "%%a"=="DB_URL" set "DB_URL=%%b"
    )
)

set "LOCAL_DB_CONNECT=!DB_URL!"
set "LOCAL_DB_CONNECT=!LOCAL_DB_CONNECT:*@=!"
set "LOCAL_BOOTSTRAP_SQL=%TEMP%\psm_smart_bootstrap_%RANDOM%.sql"
set "LOCAL_CHECK_SQL=%TEMP%\psm_smart_check_%RANDOM%.sql"
set "LOCAL_DB_SCRIPT_DIR=%~dp0db"
if not exist "!LOCAL_DB_SCRIPT_DIR!\oracle21c-init.sql" set "LOCAL_DB_SCRIPT_DIR=%ROOT_DIR%\Core\src\main\resources\db"
if not exist "!LOCAL_DB_SCRIPT_DIR!\oracle21c-init.sql" (
    echo [Error] %RED%Database init scripts were not found in release\db or Core\src\main\resources\db.%RESET%
    exit /b 1
)

echo [Status] %YELLOW%Preparing local Oracle schema user !DB_USERNAME!...%RESET%
(
    echo SET DEFINE OFF
    echo WHENEVER SQLERROR EXIT SQL.SQLCODE
    echo DECLARE
    echo   v_count NUMBER;
    echo BEGIN
    echo   SELECT COUNT^(^*^) INTO v_count FROM ALL_USERS WHERE USERNAME = UPPER^('!DB_USERNAME!'^);
    echo   IF v_count = 0 THEN
    echo     EXECUTE IMMEDIATE 'CREATE USER !DB_USERNAME! IDENTIFIED BY "!DB_PASSWORD!"';
    echo   END IF;
    echo END;
    echo /
    echo GRANT CONNECT, RESOURCE TO !DB_USERNAME!;
    echo ALTER USER !DB_USERNAME! QUOTA UNLIMITED ON USERS;
    echo EXIT
) > "!LOCAL_BOOTSTRAP_SQL!"

sqlplus -L "/ as sysdba" @"!LOCAL_BOOTSTRAP_SQL!" > nul
if !errorlevel! neq 0 (
    echo [Status] %YELLOW%OS authentication failed, trying system password...%RESET%
    sqlplus -L "system/!ORACLE_PASSWORD!@!LOCAL_DB_CONNECT!" @"!LOCAL_BOOTSTRAP_SQL!" > nul
    if !errorlevel! neq 0 (
        echo [Error] %RED%Failed to prepare local Oracle user. Check ORACLE_PASSWORD in .env.%RESET%
        exit /b 1
    )
)

(
    echo SET DEFINE OFF
    echo WHENEVER SQLERROR EXIT SQL.SQLCODE
    echo DECLARE
    echo   v_count NUMBER;
    echo BEGIN
    echo   SELECT COUNT^(^*^) INTO v_count FROM USER_TABLES WHERE TABLE_NAME = 'STATION_BASE';
    echo   IF v_count = 0 THEN
    echo     RAISE_APPLICATION_ERROR^(-20001, 'SCHEMA_MISSING'^);
    echo   END IF;
    echo END;
    echo /
    echo EXIT
) > "!LOCAL_CHECK_SQL!"

sqlplus -L "!DB_USERNAME!/!DB_PASSWORD!@!LOCAL_DB_CONNECT!" @"!LOCAL_CHECK_SQL!" > nul
if !errorlevel! neq 0 (
    echo [Status] %YELLOW%Local schema is empty, initializing tables and base data...%RESET%
    pushd "!LOCAL_DB_SCRIPT_DIR!"
    sqlplus -L "!DB_USERNAME!/!DB_PASSWORD!@!LOCAL_DB_CONNECT!" @oracle21c-init.sql
    if !errorlevel! neq 0 (
        popd
        echo [Error] %RED%Failed to initialize local Oracle schema.%RESET%
        exit /b 1
    )
    popd
) else (
    echo [Status] %GREEN%Local Oracle schema already exists.%RESET%
)

exit /b 0
