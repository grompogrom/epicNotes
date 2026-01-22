# LLM Model Setup Instructions

## Problem: "Unrecognized model path" Error

If you're seeing the error `"Unrecognized model path: models/gemma-2b-it-cpu-int8.bin"`, it's because **MediaPipe LLM Inference API requires models in `.task` format**, not `.bin` files.

## Debug Evidence

From runtime logs:
```
DEBUG_H1_H3: Model path check: modelPath=models/gemma-2b-it-cpu-int8.bin, 
             assetsInModels=[gemma-2b-it-cpu-int8.bin]
DEBUG_H2_H4: Model file accessible: fileSize=2147483647bytes (2047MB)
ERROR: MediaPipeException: Unrecognized model path: models/gemma-2b-it-cpu-int8.bin
```

**Root Cause**: The `.bin` file is accessible but MediaPipe doesn't recognize the format. MediaPipe requires `.task` files which include model weights + tokenizer + metadata in a specific format.

---

## Solution: Download MediaPipe-Compatible Model

### Step 1: Download the Correct Model Format

**From Kaggle** (Recommended):
1. Go to: https://www.kaggle.com/models/google/gemma/tfLite
2. Look for "MediaPipe" or "Android" versions
3. Download one of these models:

| Model File | Size | Best For | RAM Required |
|------------|------|----------|--------------|
| `gemma-2b-it-cpu-int4.task` | ~1.2GB | Most devices | 3-4GB RAM |
| `gemma-2b-it-cpu-int8.task` | ~2.3GB | Better quality | 6GB+ RAM |

**Alternative Sources**:
- Google AI Edge: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
- MediaPipe Samples: https://github.com/google-ai-edge/mediapipe-samples

### Step 2: Replace the Model File

1. **Remove the old `.bin` file**:
   ```bash
   rm app/src/main/assets/models/gemma-2b-it-cpu-int8.bin
   ```

2. **Add the new `.task` file**:
   - Place the downloaded `.task` file in: `app/src/main/assets/models/`
   - Example: `app/src/main/assets/models/gemma-2b-it-cpu-int8.task`

### Step 3: Update Configuration (Already Done)

The `LlmConfig.kt` file has been updated to point to `.task` instead of `.bin`:

```kotlin
val modelPath: String = "models/gemma-2b-it-cpu-int8.task"
```

### Step 4: Rebuild and Test

```bash
./gradlew installDebug
adb shell am start -n com.epicnotes.chat/.MainActivity
```

---

## Alternative: Use Different Inference Library

If you want to keep using your `.bin` file, you'll need to switch from MediaPipe to a different inference library like:

### Option: llama.cpp for Android

**Pros**:
- Supports `.bin`, `.gguf`, and other formats
- Often faster than MediaPipe
- More flexible quantization options

**Cons**:
- Requires different dependencies and implementation
- Not officially supported by Google
- May need NDK/JNI integration

**Implementation**:
Would require replacing:
- MediaPipe dependencies → llama.cpp Android bindings
- `ModelManager` implementation → llama.cpp initialization
- `OnDeviceLlmClient` → llama.cpp inference calls

---

## Quick Test: Verify Model Format

To check if your model is in the correct format:

```bash
# Check file extension
file app/src/main/assets/models/gemma-2b-it-cpu-int8.*

# MediaPipe .task files are typically FlatBuffer format
# If the output shows "data" or "binary", check the extension
```

**Correct**: `gemma-2b-it-cpu-int8.task: data` ✅  
**Wrong**: `gemma-2b-it-cpu-int8.bin: data` ❌

---

## Expected Behavior After Fix

Once you have the correct `.task` file:

1. **First message** (model loading):
   - Progress: "Initializing AI model..."
   - Time: 5-10 seconds
   - Memory: App will use ~2-3GB RAM

2. **Subsequent messages**:
   - Progress: "Generating response..."
   - Time: 2-5 seconds per response
   - Responses will be actual AI-generated text

---

## Troubleshooting

### Model still won't load?

1. **Check file size**: Ensure the downloaded file isn't corrupted
   ```bash
   ls -lh app/src/main/assets/models/
   ```
   Should show ~1.2GB or ~2.3GB

2. **Check file in APK**:
   ```bash
   adb pull /data/app/~~*/com.epicnotes.chat-*/base.apk
   unzip -l base.apk | grep models
   ```
   Should list your `.task` file

3. **Check logs**:
   ```bash
   adb logcat | grep -E "ModelManager|DEBUG_"
   ```
   Look for "Model initialized successfully" message

### Device runs out of memory?

If you see OOM errors:
- Use the smaller `int4` model (1.2GB) instead of `int8` (2.3GB)
- Close other apps before running
- Consider devices with 6GB+ RAM for best experience

---

## Summary

**Problem**: `.bin` files are not compatible with MediaPipe  
**Solution**: Download `.task` format model from Kaggle or Google AI Edge  
**Status**: Code updated to use `.task`, waiting for correct model file  

Once you download and place the `.task` file, the app will work with the on-device LLM!
