#!/bin/bash
# Script de build rápido para v2.0.0

echo "📱 Building ClowBot v2.0.0..."

# 1. Actualizar version en build.gradle.kts
sed -i 's/versionCode = 3/versionCode = 4/' app/build.gradle.kts
sed -i 's/versionName = "1.1.0"/versionName = "2.0.0"/' app/build.gradle.kts

# 2. Build APK
./gradlew assembleDebug --no-daemon

# 3. Copiar APK
if [ -f app/build/outputs/apk/debug/app-debug.apk ]; then
    cp app/build/outputs/apk/debug/app-debug.apk /opt/server-data/webs/releases/sbk-monitor-v2.0.0.apk
    echo "✅ APK creado: /opt/server-data/webs/releases/sbk-monitor-v2.0.0.apk"
    ls -lh /opt/server-data/webs/releases/sbk-monitor-v2.0.0.apk
else
    echo "❌ Error: APK no generado"
    exit 1
fi
