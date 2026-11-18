@echo off
REM Mini Bank: double-click to start the API on your laptop and open Swagger UI in your browser.
REM Uses a small built-in database (H2) saved in the "data" folder, so you don't need PostgreSQL or Docker.
setlocal EnableExtensions
cd /d "%~dp0"
echo.
echo ===== Mini Bank =====
echo.

REM ---- Find Java 21 or newer, or download a private copy just for this project ----
set "JAVA_OK="
if exist ".tools\jdk\bin\java.exe" (
    set "JAVA_HOME=%CD%\.tools\jdk"
    set "PATH=%CD%\.tools\jdk\bin;%PATH%"
    set "JAVA_OK=1"
)
if not defined JAVA_OK (
    java -XshowSettings:properties -version 2>&1 | findstr /r /c:"java.specification.version = 2[1-9]" >nul && set "JAVA_OK=1"
)
if not defined JAVA_OK (
    echo Java 21 isn't installed, so downloading a copy just for this project ^(about 190 MB, first time only^)...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$ErrorActionPreference='Stop'; New-Item -ItemType Directory -Force .tools | Out-Null;" ^
      "Invoke-WebRequest 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile .tools\jdk.zip;" ^
      "Expand-Archive .tools\jdk.zip .tools\jdk-tmp -Force; Move-Item (Get-ChildItem .tools\jdk-tmp)[0].FullName .tools\jdk;" ^
      "Remove-Item .tools\jdk-tmp, .tools\jdk.zip -Recurse -Force"
    if not exist ".tools\jdk\bin\java.exe" goto nojava
    set "JAVA_HOME=%CD%\.tools\jdk"
    set "PATH=%CD%\.tools\jdk\bin;%PATH%"
)

REM ---- Maven wrapper settings (recreated if missing) ----
if not exist ".mvn\wrapper\maven-wrapper.properties" (
    if not exist ".mvn\wrapper" mkdir ".mvn\wrapper"
    > ".mvn\wrapper\maven-wrapper.properties" echo wrapperVersion=3.3.2
    >> ".mvn\wrapper\maven-wrapper.properties" echo distributionType=only-script
    >> ".mvn\wrapper\maven-wrapper.properties" echo distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip
)

echo Starting the bank. The first run downloads Maven and the libraries, so give it a few minutes.
echo Swagger UI will open in your browser once it's up: http://localhost:8080/swagger-ui.html
echo Leave this window open while you use it. Close it to stop the bank.
echo.
start "" /min powershell -NoProfile -Command "for($i=0;$i -lt 180;$i++){try{Invoke-WebRequest -UseBasicParsing http://localhost:8080/actuator/health | Out-Null; Start-Process 'http://localhost:8080/swagger-ui.html'; break}catch{Start-Sleep 2}}"
call mvnw.cmd -q spring-boot:run -Dspring-boot.run.profiles=local
pause
exit /b 0

:nojava
echo.
echo Couldn't download Java. Install "Temurin 21" from https://adoptium.net and run this again.
pause
exit /b 1
