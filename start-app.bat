@echo off
echo Starting Vendor Payment Notifier Application...
call mvn spring-boot:run -Dspring-boot.run.profiles=dev
pause
