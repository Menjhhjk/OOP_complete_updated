@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
set MAVEN_WRAPPER_JAR=%CD%\.mvn\wrapper\maven-wrapper.jar
set JAVA_CMD=java
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java"
    )
)
"%JAVA_CMD%" -classpath "%MAVEN_WRAPPER_JAR%" -Dmaven.multiModuleProjectDirectory="%CD%" org.apache.maven.wrapper.MavenWrapperMain %*
