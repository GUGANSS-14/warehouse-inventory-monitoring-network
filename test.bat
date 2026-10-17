@echo off
setlocal
echo ===============================================================================
echo Running WIMN Computer Networks Automated Test Suite
echo ===============================================================================

set "JAVA_HOME=%~dp0.tools\jdk-21.0.6+7"
set "PATH=%JAVA_HOME%\bin;%~dp0.tools\apache-maven-3.9.9\bin;%PATH%"

call mvn test
pause
