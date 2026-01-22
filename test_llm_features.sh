#!/bin/bash

# Test script for on-device LLM features
# Tests the monitoring, error handling, and UI enhancements

PACKAGE="com.epicnotes.chat"
ACTIVITY=".MainActivity"

echo "════════════════════════════════════════════════════════════"
echo "  Epic Notes - LLM Integration Testing Script"
echo "════════════════════════════════════════════════════════════"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

# 1. Check device connection
print_status "Checking device connection..."
DEVICE=$(adb devices | grep -w "device" | wc -l)
if [ $DEVICE -eq 0 ]; then
    print_error "No device connected!"
    exit 1
fi
print_success "Device connected"

# 2. Check if app is installed
print_status "Checking if app is installed..."
if adb shell pm list packages | grep -q "$PACKAGE"; then
    print_success "App is installed"
else
    print_error "App not installed!"
    exit 1
fi

# 3. Check app process
print_status "Checking if app is running..."
if adb shell "ps -A | grep $PACKAGE" > /dev/null 2>&1; then
    MEMORY=$(adb shell "ps -A | grep $PACKAGE" | awk '{print $6}')
    print_success "App is running (Memory: ${MEMORY}KB)"
else
    print_info "App not running, launching..."
    adb shell am start -n "${PACKAGE}${ACTIVITY}"
    sleep 2
fi

# 4. Check device RAM
print_status "Checking device specifications..."
TOTAL_MEM=$(adb shell cat /proc/meminfo | grep MemTotal | awk '{print int($2/1024)}')
AVAIL_MEM=$(adb shell cat /proc/meminfo | grep MemAvailable | awk '{print int($2/1024)}')
print_info "Device RAM: ${TOTAL_MEM}MB total, ${AVAIL_MEM}MB available"

if [ $TOTAL_MEM -lt 3072 ]; then
    print_error "⚠️  Device has less than 3GB RAM - may have issues running the model"
elif [ $TOTAL_MEM -lt 4096 ]; then
    print_info "⚠️  Device has ${TOTAL_MEM}MB RAM - 4GB+ recommended for best performance"
else
    print_success "Device has sufficient RAM (${TOTAL_MEM}MB)"
fi

# 5. Check if model file exists
print_status "Checking if model file exists in assets..."
MODEL_PATH="/data/local/tmp/test_model_check"
adb pull "/data/app/~~*/com.epicnotes.chat-*/base.apk" "$MODEL_PATH.apk" 2>/dev/null
if [ -f "$MODEL_PATH.apk" ]; then
    if unzip -l "$MODEL_PATH.apk" 2>/dev/null | grep -q "models/gemma"; then
        MODEL_SIZE=$(unzip -l "$MODEL_PATH.apk" 2>/dev/null | grep "models/gemma" | awk '{print $1}')
        print_success "Model file found in APK (Size: ${MODEL_SIZE} bytes)"
    else
        print_error "Model file NOT found in APK!"
        print_info "This is expected if you haven't added the model file yet"
    fi
    rm -f "$MODEL_PATH.apk"
else
    print_info "Could not check APK contents"
fi

# 6. Monitor logs in background
print_status "Starting log monitor..."
LOG_FILE="/tmp/epicnotes_test_log.txt"
adb logcat -c
adb logcat | grep -E "(OnDeviceLlmClient|ModelManager|LlmMetrics|ChatViewModel|epicnotes)" > "$LOG_FILE" &
LOGCAT_PID=$!
print_success "Log monitor started (PID: $LOGCAT_PID)"

# 7. Instructions for manual testing
echo ""
echo "════════════════════════════════════════════════════════════"
echo -e "${GREEN}Manual Testing Instructions:${NC}"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "The app should now be visible on your device."
echo ""
echo -e "${YELLOW}Test Scenarios:${NC}"
echo ""
echo "1. ${BLUE}Test Normal Message Flow:${NC}"
echo "   - Type a message: 'Hello, how are you?'"
echo "   - Click Send"
echo "   - Observe: Progress indicator should appear"
echo "   - Check: 'Initializing AI model...' message (first time)"
echo "   - Verify: Countdown timer (10s for first message)"
echo "   - Result: Response should appear or error if no model"
echo ""
echo "2. ${BLUE}Test Cancellation:${NC}"
echo "   - Send another message"
echo "   - Click the red 'Cancel' button during processing"
echo "   - Verify: Returns to ready state without error"
echo ""
echo "3. ${BLUE}Test Multiple Messages:${NC}"
echo "   - Send 2-3 messages in succession"
echo "   - Verify: Subsequent messages show 5s estimate (not 10s)"
echo "   - Check: Responses are faster (no model loading)"
echo ""
echo "4. ${BLUE}Test Error Messages:${NC}"
echo "   - If model is missing, should see friendly error"
echo "   - Error should be clear and actionable"
echo ""
echo "5. ${BLUE}Visual Checks:${NC}"
echo "   ✓ Progress card appears above input field"
echo "   ✓ Circular spinner animates"
echo "   ✓ Countdown timer updates every second"
echo "   ✓ Cancel button is red and prominent"
echo "   ✓ Input field is disabled during processing"
echo "   ✓ Messages display in proper bubbles"
echo ""
echo "════════════════════════════════════════════════════════════"
echo -e "${YELLOW}Monitoring Logs...${NC}"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "Press Ctrl+C when done testing"
echo ""

# 8. Monitor and display relevant logs
tail -f "$LOG_FILE" 2>/dev/null &
TAIL_PID=$!

# Wait for user to finish testing
trap "kill $LOGCAT_PID $TAIL_PID 2>/dev/null; rm -f $LOG_FILE; echo ''; echo 'Testing stopped.'; exit 0" INT

wait

