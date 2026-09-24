@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF)
@REM Maven wrapper script for Windows
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0")
@SET MAVEN_PROJECTBASEDIR=%BASE_DIR%

@SET MAVEN_OPTS=-Xmx512m

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") DO (
    @IF "%%A"=="distributionUrl" SET DISTRIBUTION_URL=%%B
)

@SET MAVEN_USER_HOME=%USERPROFILE%\.m2\wrapper
@SET MVN_CMD=%MAVEN_USER_HOME%\dists\apache-maven-3.9.6\bin\mvn.cmd

@IF NOT EXIST "%MVN_CMD%" (
    @ECHO Downloading Maven 3.9.6...
    @MKDIR "%MAVEN_USER_HOME%\dists" 2>NUL
    @powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile '%MAVEN_USER_HOME%\maven.zip'; Expand-Archive -Path '%MAVEN_USER_HOME%\maven.zip' -DestinationPath '%MAVEN_USER_HOME%\dists' -Force; Remove-Item '%MAVEN_USER_HOME%\maven.zip'"
)

@SET JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot
@"%MVN_CMD%" %*
