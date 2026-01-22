#!/bin/bash

# EpicNotes LLM Diagnostic Script
# This script checks common issues with the Android LLM app

set -e

APP_PACKAGE="com.epicnotes.chat"
MODEL_FILE="llama-3.2-3b-instruct-q4_k_m.gguf"
EXPECTED_MODEL_SIZE_MB=2048

echo "═══════════════════════════════════════════════════════"
echo "  EpicNotes LLM Diagnostic Tool"
echo "═══════════════════════════════════════════════════════"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check functions
check_pass() {
    echo -e "${GREEN}✓${NC} $1"
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# 1. Check if device is connected
echo "1. Checking device connection..."
if adb devices | grep -q "device$"; then
    check_pass "Device connected"
    DEVICE_MODEL=$(adb shell getprop ro.product.model | tr -d '\r')
    echo "   Device: $DEVICE_MODEL"
else
    check_fail "No device connected"
    echo "   Please connect an Android device via USB or wireless"
    exit 1
fi

# 2. Check if app is installed
echo ""
echo "2. Checking app installation..."
if adb shell pm list packages | grep -q "$APP_PACKAGE"; then
    check_pass "App installed"
    APP_VERSION=$(adb shell dumpsys package $APP_PACKAGE | grep versionName | head -1 | awk -F'=' '{print $2}')
    echo "   Package: $APP_PACKAGE"
else
    check_fail "App not installed"
    echo "   Please install the app first: ./gradlew installDebug"
    exit 1
fi

# 3. Check device architecture
echo ""
echo "3. Checking device architecture..."
DEVICE_ABI=$(adb shell getprop ro.product.cpu.abi | tr -d '\r')
echo "   CPU ABI: $DEVICE_ABI"
if [[ "$DEVICE_ABI" == "arm64-v8a" ]]; then
    check_pass "arm64-v8a supported (compatible with llama.cpp)"
elif [[ "$DEVICE_ABI" == "armeabi-v7a" ]]; then
    check_warn "armeabi-v7a detected (32-bit ARM, may be slower)"
else
    check_fail "Unsupported architecture: $DEVICE_ABI"
    echo "   llama.cpp requires ARM64 or ARM32"
fi

# 4. Check device memory
echo ""
echo "4. Checking device memory..."
MEM_TOTAL_KB=$(adb shell cat /proc/meminfo | grep MemTotal | awk '{print $2}')
MEM_TOTAL_GB=$((MEM_TOTAL_KB / 1024 / 1024))
MEM_AVAIL_KB=$(adb shell cat /proc/meminfo | grep MemAvailable | awk '{print $2}')
MEM_AVAIL_GB=$((MEM_AVAIL_KB / 1024 / 1024))
echo "   Total RAM: ${MEM_TOTAL_GB}GB"
echo "   Available RAM: ${MEM_AVAIL_GB}GB"
if [ $MEM_TOTAL_GB -ge 4 ]; then
    check_pass "Sufficient RAM (${MEM_TOTAL_GB}GB)"
else
    check_warn "Low RAM (${MEM_TOTAL_GB}GB) - 4GB+ recommended"
fi

# 5. Check storage
echo ""
echo "5. Checking storage space..."
DATA_FREE_MB=$(adb shell df /data | tail -1 | awk '{print $4}')
DATA_FREE_GB=$((DATA_FREE_MB / 1024 / 1024))
echo "   Free space: ${DATA_FREE_GB}GB"
if [ $DATA_FREE_GB -ge 3 ]; then
    check_pass "Sufficient storage (${DATA_FREE_GB}GB free)"
else
    check_fail "Insufficient storage (${DATA_FREE_GB}GB free) - need 2GB+ for model"
fi

# 6. Check if model file exists
echo ""
echo "6. Checking model file..."
MODEL_PATH="/data/data/$APP_PACKAGE/files/models/$MODEL_FILE"
if adb shell "run-as $APP_PACKAGE test -f $MODEL_PATH && echo exists" | grep -q "exists"; then
    check_pass "Model file found"
    MODEL_SIZE=$(adb shell "run-as $APP_PACKAGE stat -c %s $MODEL_PATH" | tr -d '\r')
    MODEL_SIZE_MB=$((MODEL_SIZE / 1024 / 1024))
    echo "   Path: $MODEL_PATH"
    echo "   Size: ${MODEL_SIZE_MB}MB"
    
    if [ $MODEL_SIZE_MB -ge 1900 ] && [ $MODEL_SIZE_MB -le 2200 ]; then
        check_pass "Model size looks correct (${MODEL_SIZE_MB}MB)"
    else
        check_warn "Model size unexpected (${MODEL_SIZE_MB}MB, expected ~2048MB)"
        echo "   Model may be corrupted or incomplete"
    fi
else
    check_fail "Model file not found"
    echo "   Expected path: $MODEL_PATH"
    echo "   The app should download it on first launch"
    echo "   Or use: ./DEBUG_GUIDE.md for manual download instructions"
fi

# 7. Check native libraries in APK
echo ""
echo "7. Checking native libraries..."
APK_PATH=$(adb shell pm path $APP_PACKAGE | cut -d: -f2 | tr -d '\r')
echo "   APK: $APK_PATH"

# Create temp directory for APK inspection
TEMP_DIR=$(mktemp -d)
adb pull "$APK_PATH" "$TEMP_DIR/app.apk" > /dev/null 2>&1

LIBS_FOUND=0
for lib in "libllama_jni.so" "libllama.so" "libggml.so" "libggml-base.so" "libggml-cpu.so"; do
    if unzip -l "$TEMP_DIR/app.apk" | grep -q "lib/$DEVICE_ABI/$lib"; then
        check_pass "$lib found in APK"
        LIBS_FOUND=$((LIBS_FOUND + 1))
    else
        check_fail "$lib NOT found in APK"
    fi
done

# Cleanup
rm -rf "$TEMP_DIR"

if [ $LIBS_FOUND -eq 5 ]; then
    check_pass "All native libraries present"
else
    check_warn "Missing libraries - rebuild may be needed"
fi

# 8. Check SharedPreferences
echo ""
echo "8. Checking app state..."
PREFS_FILE="/data/data/$APP_PACKAGE/shared_prefs/chat_app_prefs.xml"
if adb shell "run-as $APP_PACKAGE test -f $PREFS_FILE && echo exists" | grep -q "exists"; then
    FIRST_START=$(adb shell "run-as $APP_PACKAGE cat $PREFS_FILE" | grep "first_start_done" | grep -o 'value="[^"]*"' | cut -d'"' -f2)
    if [ "$FIRST_START" = "true" ]; then
        check_pass "First start completed"
    else
        check_warn "First start not completed (model may still be downloading)"
    fi
else
    check_warn "App not run yet (SharedPreferences not found)"
fi

# 9. Check logcat for errors
echo ""
echo "9. Checking recent logs for errors..."
ERRORS=$(adb logcat -d -s $APP_PACKAGE:E AndroidRuntime:E | tail -5)
if [ -z "$ERRORS" ]; then
    check_pass "No recent errors in logcat"
else
    check_warn "Recent errors found:"
    echo "$ERRORS" | sed 's/^/   /'
fi

# 10. Check if app is using Local or Mock client
echo ""
echo "10. Checking LLM client configuration..."
USE_LOCAL=$(grep -r "useLocalLlama.*true" app/src/main/java/com/epicnotes/chat/presentation/di/AppModule.kt)
if [ -n "$USE_LOCAL" ]; then
    check_pass "Using LocalLlamaLlmClient (on-device inference)"
else
    check_warn "Using MockLlmClient (for testing only)"
    echo "   Set useLocalLlama = true in AppModule.kt"
fi

# Summary
echo ""
echo "═══════════════════════════════════════════════════════"
echo "  Summary"
echo "═══════════════════════════════════════════════════════"

CRITICAL_ISSUES=0

# Critical checks
if [ $MEM_TOTAL_GB -lt 4 ]; then
    echo -e "${RED}✗ Insufficient RAM${NC} - 4GB+ required for good performance"
    CRITICAL_ISSUES=$((CRITICAL_ISSUES + 1))
fi

if [ $DATA_FREE_GB -lt 3 ]; then
    echo -e "${RED}✗ Insufficient storage${NC} - need 2GB+ for model"
    CRITICAL_ISSUES=$((CRITICAL_ISSUES + 1))
fi

if ! adb shell "run-as $APP_PACKAGE test -f $MODEL_PATH && echo exists" | grep -q "exists"; then
    echo -e "${RED}✗ Model file missing${NC} - app needs to download it (requires network)"
    CRITICAL_ISSUES=$((CRITICAL_ISSUES + 1))
fi

if [ $LIBS_FOUND -lt 5 ]; then
    echo -e "${RED}✗ Native libraries missing${NC} - rebuild the app: ./gradlew clean assembleDebug"
    CRITICAL_ISSUES=$((CRITICAL_ISSUES + 1))
fi

if [ $CRITICAL_ISSUES -eq 0 ]; then
    echo -e "${GREEN}✓ All critical checks passed!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Open the app and send a message"
    echo "2. Monitor logs: adb logcat | grep -E 'ChatApp|Llama|Model'"
    echo "3. If issues persist, check DEBUG_GUIDE.md for detailed troubleshooting"
else
    echo -e "${RED}✗ Found $CRITICAL_ISSUES critical issue(s)${NC}"
    echo ""
    echo "Please fix the issues above before using the app."
    echo "See DEBUG_GUIDE.md for detailed solutions."
fi

echo ""
echo "═══════════════════════════════════════════════════════"
echo ""
echo "Full diagnostic log saved to: /tmp/epicnotes_diagnostic.log"
echo "To capture live logs: adb logcat | grep -E 'ChatApp|Llama|Model' | tee /tmp/llm_live.log"
echo ""
