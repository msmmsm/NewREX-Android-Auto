$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"
$env:ANDROID_HOME = "C:\Users\murat\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = "C:\Users\murat\AppData\Local\Android\Sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:PATH"

Write-Host "Lokal APK derlemesi baslatiliyor (Full & Park)..."
cmd.exe /c "gradlew.bat --no-daemon assembleFullDebug assembleParkDebug"

