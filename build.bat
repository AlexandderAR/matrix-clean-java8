@echo off
setlocal
if "%~2"=="" (
  echo Usage: build.bat "C:\path\jdk8" "C:\path\jdk21"
  exit /b 2
)
set "JAVA_HOME=%~f1"
set "MATRIX_JAVA8_HOME=%~f1"
set "MATRIX_COMPILER_JAVA_HOME=%~f2"
if not exist "%JAVA_HOME%\bin\javac.exe" (
  echo JDK 8 javac.exe not found.
  exit /b 2
)
if not exist "%MATRIX_COMPILER_JAVA_HOME%\bin\javac.exe" (
  echo JDK 21 javac.exe not found.
  exit /b 2
)
pushd "%~dp0"
if errorlevel 1 exit /b 2
call "%~dp0tools\gradle-8.14.3\bin\gradle.bat" --offline --no-daemon assemble
set "BUILD_RESULT=%ERRORLEVEL%"
popd
exit /b %BUILD_RESULT%
