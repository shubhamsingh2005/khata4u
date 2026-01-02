@echo off
REM start-ngrok.bat
REM Starts ngrok for local port 8080 and prints the public HTTPS URL (copies it to clipboard on Windows)

:: Check for ngrok in PATH
where ngrok >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ngrok not found in PATH. Download from https://ngrok.com and place ngrok.exe on your PATH or in this folder.
    pause
    exit /b 1
)

:: Start ngrok in background if not already running on this port
REM Check if ngrok already has a tunnel for 8080 by querying local API
powershell -Command "try { (Invoke-RestMethod -Uri http://127.0.0.1:4040/api/tunnels -UseBasicParsing).tunnels | Where-Object { $_.config.addr -like '*:8080' } } catch { $null }" > "%TEMP%\ngrok_tunnels.json" 2>&1
nfor /f "usebackq delims=" %%A in ("%TEMP%\ngrok_tunnels.json") do set NGROK_EXISTS=%%A
if not "%NGROK_EXISTS%"=="" (
    echo ngrok tunnel already running for port 8080.
) else (
    echo Starting ngrok for port 8080...
    start "ngrok" ngrok http 8080
    echo Waiting 2 seconds for ngrok to initialize...
    timeout /t 2 /nobreak >nul
)

:: Query ngrok API to get public URL
set RETRY=0
:GETURL
powershell -Command "try { Invoke-RestMethod -Uri http://127.0.0.1:4040/api/tunnels -UseBasicParsing | ConvertTo-Json -Depth 5 } catch { Write-Output '' }" > "%TEMP%\ngrok_tunnels.json" 2>&1
set /p TUNNELS=<"%TEMP%\ngrok_tunnels.json"
if "%TUNNELS%"=="" (
    set /a RETRY+=1
    if %RETRY% GEQ 10 (
        echo Failed to get ngrok tunnels after multiple attempts.
        type "%TEMP%\ngrok_tunnels.json"
        pause
        exit /b 1
    )
    timeout /t 1 /nobreak >nul
    goto GETURL
)

:: Extract https public_url using Powershell for robust JSON parsing
for /f "delims=" %%U in ('powershell -Command "(Get-Content -Raw \"%TEMP%\ngrok_tunnels.json\") | ConvertFrom-Json | Select-Object -ExpandProperty tunnels | Where-Object { $_.proto -eq 'https' -or $_.public_url -like 'https:*' } | Select-Object -First 1 -ExpandProperty public_url"') do set NGROK_URL=%%U
if "%NGROK_URL%"=="" (
    for /f "delims=" %%U in ('powershell -Command "(Get-Content -Raw \"%TEMP%\ngrok_tunnels.json\") | ConvertFrom-Json | Select-Object -ExpandProperty tunnels | Select-Object -First 1 -ExpandProperty public_url"') do set NGROK_URL=%%U
)
if "%NGROK_URL%"=="" (
    echo Could not parse ngrok public URL. See "%TEMP%\ngrok_tunnels.json"
    type "%TEMP%\ngrok_tunnels.json"
    pause
    exit /b 1
)

:: Print and copy to clipboard
echo ngrok public URL: %NGROK_URL%
echo %NGROK_URL%| clip
echo (Copied to clipboard)
pause

