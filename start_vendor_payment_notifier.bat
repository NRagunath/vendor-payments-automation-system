@echo off
setlocal enabledelayedexpansion

title Shanthi Gear - Vendor Payment Notifier
echo Starting Vendor Payment Notification System...
echo.

REM Set Java Home to Java 21
set JAVA_HOME=D:\java-21
set PATH=%JAVA_HOME%\bin;%PATH%

REM Set Maven path directly
set MAVEN_HOME="C:\Program Files\apache-maven-3.9.10"
set PATH=%MAVEN_HOME%\bin;%PATH%

REM Verify paths
echo Java: %JAVA_HOME%
echo Maven: %MAVEN_HOME%
echo.

REM Create logs directory if it doesn't exist
if not exist "logs" mkdir logs

REM Verify Maven installation
echo Verifying Maven installation...
"%MAVEN_HOME%\bin\mvn.cmd" -version

REM Run the application
echo.
echo Starting application...
java -jar target/vendor-payment-notifier-1.0-SNAPSHOT.jar