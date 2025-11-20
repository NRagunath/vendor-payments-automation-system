@echo off
REM This script sets up environment variables for Vendor Payment Notifier
setlocal enabledelayedexpansion

echo ===========================================
echo  Vendor Payment Notifier - Environment Setup
echo ===========================================

set "ENV_FILE=.env"

:check_env
if exist "%ENV_FILE%" (
    echo [INFO] .env file already exists.
    echo [INFO] Backing up existing .env to .env.bak...
    copy "%ENV_FILE%" "%ENV_FILE%.bak" >nul
    echo [INFO] Creating new .env file with default configuration...
) else (
    echo [INFO] Creating new .env file...
)

(
    echo # HDFC Bank API Configuration
    echo BANK_API_URL=https://api.hdfcbank.com
    echo BANK_CLIENT_ID=your_actual_client_id
    echo BANK_CLIENT_SECRET=your_actual_client_secret
    echo BANK_API_KEY=your_actual_api_key
    echo.
    echo # Webhook Configuration
    echo BANK_WEBHOOK_URL=https://yourdomain.com/api/webhooks
    echo BANK_WEBHOOK_SECRET=your_webhook_secret
    echo.
    echo # SSL Configuration
    echo SSL_KEYSTORE_PATH=C:\\path\\to\\keystore.jks
    echo SSL_KEYSTORE_PASSWORD=your_keystore_password
    echo SSL_TRUSTSTORE_PATH=C:\\path\\to\\truststore.jks
    echo SSL_TRUSTSTORE_PASSWORD=your_truststore_password
) > "%ENV_FILE%"

echo [SUCCESS] .env file has been created/updated.
echo [INFO] Please edit the .env file with your actual credentials.
echo.

echo Loading environment variables...
for /f "usebackq tokens=1,2 delims==" %%a in ("%ENV_FILE%") do (
    if not "%%a"=="" if not "%%a"=="#" (
        set "%%a=%%b"
        echo [SET] %%a=*****
    )
)

echo.
echo ===========================================
echo  Environment Setup Complete!
echo  Please update the .env file with your actual credentials
echo ===========================================

endlocal

pause
