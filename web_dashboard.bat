@echo off
setlocal
echo ===============================================================================
echo Starting WIMN Web Dashboard on http://localhost:8080
echo ===============================================================================

set "JAVA_HOME=%~dp0.tools\jdk-21.0.6+7"
set "PATH=%JAVA_HOME%\bin;%~dp0.tools\apache-maven-3.9.9\bin;%PATH%"

call mvn compile exec:java -Dexec.mainClass="com.wimn.StandaloneWebLauncher"
pause
