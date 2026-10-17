@echo off
setlocal
echo ===============================================================================
echo Starting Warehouse Inventory Monitoring Network (WIMN)
echo Course: BE23CS402 - Computer Networks Capstone Simulation
echo ===============================================================================

set "JAVA_HOME=%~dp0.tools\jdk-21.0.6+7"
set "PATH=%JAVA_HOME%\bin;%~dp0.tools\apache-maven-3.9.9\bin;%PATH%"

call mvn javafx:run
pause
