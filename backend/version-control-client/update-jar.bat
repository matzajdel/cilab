

echo ==========================================
echo 1. Building new version (Maven)...
echo ==========================================

cd /d "C:\Users\mateu\Desktop\CILab\cilab\version-control-client"
call mvn clean package

if errorlevel 1 (
    echo BUILD FAILED
    pause
    exit /b %errlevel%
)

echo ==========================================
echo 2. Install new version...
echo ==========================================

copy /Y "target\version-control-client-1.0-SNAPSHOT.jar" "C:\Users\mateu\Desktop\CILab\cilab-tools\myvcs-client.jar"

echo.
echo ==========================================
echo SUCCESS!
echo ==========================================
pause