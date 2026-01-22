# Critical Issue: Model File Format

## Problem Identified

The file `gemma3-1b-it-int8-web.task` is **NOT a valid ZIP archive**. 

**Evidence:**
```
$ unzip -t gemma3-1b-it-int8-web.task
End-of-central-directory signature not found. Either this file is not
a zipfile, or it constitutes one disk of a multi-part archive.
```

**File Header Analysis:**
- File starts with: `1c 00 00 00 54 46 4c 33` (TFL3 = TensorFlow Lite 3)
- This is a **raw TFL3 model file**, not a MediaPipe `.task` bundle

## What MediaPipe Expects

MediaPipe `.task` files must be **ZIP archives** containing:
1. The TFL3 model file
2. Tokenizer files
3. Metadata/config files
4. Other assets

The `.task` format is a **bundle/container**, not just the raw model.

## Why It Fails

When MediaPipe tries to load the file:
1. It expects a ZIP archive structure
2. Tries to open it as a ZIP
3. Fails with "unable to open zip archive" because it's not a ZIP

## Solution

You need to download a **properly packaged Android `.task` file**, not a raw TFL3 or web version.

### Option 1: Download Android-Compatible Model

Look for models specifically marked for **Android** (not "web"):
- `gemma-2b-it-cpu-int8.task` (Android)
- `gemma-2b-it-cpu-int4.task` (Android, smaller)
- `gemma-3-1b-it-cpu-int8.task` (Android, if available)

**Sources:**
- Kaggle: https://www.kaggle.com/models/google/gemma/tfLite
- Google AI Edge: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android

### Option 2: Convert TFL3 to .task Format

If you have the TFL3 file, you need to package it into a `.task` bundle using MediaPipe's conversion tools. This requires:
- MediaPipe Model Maker or conversion scripts
- Tokenizer files
- Metadata configuration

**This is complex and not recommended** - better to download a pre-packaged Android model.

## Current File Status

- ❌ `gemma3-1b-it-int8-web.task` - **WRONG FORMAT** (raw TFL3, not ZIP)
- ❌ File has "-web" suffix - suggests it's for JavaScript/Web, not Android
- ✅ File size: 964MB (complete, not truncated)
- ✅ File header: Valid TFL3 format (but not packaged as .task)

## Next Steps

1. **Download a proper Android `.task` file** from official sources
2. **Verify it's a ZIP archive**: `unzip -t <file>.task` should succeed
3. **Replace the file** in `app/src/main/assets/models/`
4. **Update `LlmConfig.kt`** with the new filename
5. **Rebuild and test**

## Verification Command

After downloading a new model, verify it's a valid ZIP:
```bash
unzip -t app/src/main/assets/models/<new-model>.task
```

Should output: `No errors detected in compressed data`
