@echo off
echo ========================================================
echo Starting local development server for OLMS (Jetty)...
echo ========================================================
echo.
echo Hot-reloading is ENABLED. If you change Java files or JSPs,
echo Jetty will automatically reload them in a few seconds.
echo.
echo To test on your mobile phone:
echo 1. Make sure your phone is on the same Wi-Fi as your PC.
echo 2. Open this URL on your phone's browser:
echo    http://192.168.0.7:8080/olms
echo.
echo Starting Maven Jetty Plugin...
mvn jetty:run -Djetty.http.host=0.0.0.0
