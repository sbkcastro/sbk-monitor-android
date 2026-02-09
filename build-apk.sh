#!/bin/bash
set -e

echo "=== SBK Monitor APK Builder ==="
echo ""

cd /opt/android-build/sbk-monitor

# Step 1: Build the Android SDK Docker image
echo "[1/4] Building Android SDK Docker image..."
docker build -f Dockerfile.build -t android-sdk-builder . 2>&1

# Step 2: Generate gradle wrapper inside container
echo "[2/4] Generating Gradle wrapper..."
docker run --rm -v /opt/android-build/sbk-monitor:/project android-sdk-builder bash -c "
cd /project
# Generate gradle wrapper
gradle wrapper --gradle-version 8.2 2>/dev/null || {
    # If gradle not installed, download wrapper manually
    mkdir -p gradle/wrapper
    wget -q https://services.gradle.org/distributions/gradle-8.2-bin.zip -O /tmp/gradle.zip
    unzip -qo /tmp/gradle.zip -d /tmp/
    /tmp/gradle-8.2/bin/gradle wrapper --gradle-version 8.2
}
chmod +x gradlew
"

# Step 3: Build the APK
echo "[3/4] Building APK (this may take several minutes)..."
docker run --rm -v /opt/android-build/sbk-monitor:/project android-sdk-builder bash -c "
cd /project
export ANDROID_SDK_ROOT=/opt/android-sdk
./gradlew assembleDebug --no-daemon --stacktrace 2>&1
"

# Step 4: Copy APK
echo "[4/4] Copying APK..."
APK_PATH="/opt/android-build/sbk-monitor/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    cp "$APK_PATH" /var/lib/lxc/lxc-sbk/rootfs/opt/apps/sbk-monitor-api/sbk-monitor.apk
    echo ""
    echo "=== BUILD SUCCESSFUL ==="
    echo "APK: $APK_PATH"
    echo "Download: curl -H 'Authorization: Bearer TOKEN' http://IP:3100/api/download/apk -o sbk-monitor.apk"
    ls -lh "$APK_PATH"
else
    echo "ERROR: APK not found at $APK_PATH"
    exit 1
fi
