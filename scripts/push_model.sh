#!/bin/bash

# Script to push large model files to Android device
# Large models (>2GB) are excluded from APK to avoid ZIP32 4GB limit

PACKAGE="com.epicnotes.chat"
MODEL_FILE="gemma2-2b-it-cpu-int8.task"
ASSETS_PATH="app/src/main/assets/models/$MODEL_FILE"
DEVICE_TMP="/data/local/tmp/$MODEL_FILE"
DEVICE_FINAL="/data/data/$PACKAGE/files/models/$MODEL_FILE"

echo "════════════════════════════════════════════════════════════"
echo "  Pushing LLM Model to Android Device"
echo "════════════════════════════════════════════════════════════"
echo ""

# Check if model file exists
if [ ! -f "$ASSETS_PATH" ]; then
    echo "❌ Error: Model file not found: $ASSETS_PATH"
    echo ""
    echo "Please download the model file and place it in:"
    echo "  $ASSETS_PATH"
    exit 1
fi

# Check file size
FILE_SIZE=$(stat -f%z "$ASSETS_PATH" 2>/dev/null || stat -c%s "$ASSETS_PATH" 2>/dev/null)
FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
echo "📦 Model file: $MODEL_FILE"
echo "📏 Size: ${FILE_SIZE_MB}MB"
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ Error: No Android device connected"
    echo "Please connect your device via USB and enable USB debugging"
    exit 1
fi

echo "📱 Device connected"
echo ""

# Step 1: Push to temporary location
echo "Step 1/3: Pushing model to device temporary storage..."
echo "  This may take several minutes for large files..."
adb push "$ASSETS_PATH" "$DEVICE_TMP"
if [ $? -ne 0 ]; then
    echo "❌ Failed to push model file"
    exit 1
fi
echo "✅ Model pushed to temporary storage"
echo ""

# Step 2: Copy to app's internal storage
echo "Step 2/3: Copying model to app's internal storage..."
adb shell "run-as $PACKAGE mkdir -p files/models"
adb shell "run-as $PACKAGE cp $DEVICE_TMP $DEVICE_FINAL"
if [ $? -ne 0 ]; then
    echo "❌ Failed to copy model to app storage"
    echo "Cleaning up temporary file..."
    adb shell "rm $DEVICE_TMP"
    exit 1
fi
echo "✅ Model copied to app storage"
echo ""

# Step 3: Clean up temporary file
echo "Step 3/3: Cleaning up temporary file..."
adb shell "rm $DEVICE_TMP"
echo "✅ Cleanup complete"
echo ""

# Verify file
echo "Verifying model file..."
DEVICE_SIZE=$(adb shell "run-as $PACKAGE stat -c%s $DEVICE_FINAL" 2>/dev/null || adb shell "run-as $PACKAGE stat -f%z $DEVICE_FINAL" 2>/dev/null)
if [ "$DEVICE_SIZE" = "$FILE_SIZE" ]; then
    echo "✅ Model file verified (${FILE_SIZE_MB}MB)"
else
    echo "⚠️  Warning: File size mismatch!"
    echo "   Expected: ${FILE_SIZE}bytes"
    echo "   Got: ${DEVICE_SIZE}bytes"
fi
echo ""

echo "════════════════════════════════════════════════════════════"
echo "  ✅ Model push complete!"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "You can now launch the app and use the LLM feature."
echo ""
