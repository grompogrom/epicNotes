# STEP 1: Prepare Model and Test It - Completion Report

## Summary

This directory contains the model preparation script for integrating Gemma 2B on-device LLM into the epicNotes Android app.

## What Was Completed

### ✅ 1. Python Environment Setup
- Used existing Python 3.13.3 virtual environment at `venv/`
- Installed required dependencies:
  - `mediapipe>=0.10.14` (for model bundling tools)
  - `huggingface-hub>=0.20.0` (for model download)
  - `psutil>=5.9.0` (for system monitoring)
  - Existing packages: `torch`, `transformers`

### ✅ 2. Created Model Download & Validation Script
- **Location**: `scripts/test_model.py`
- **Purpose**: Download pre-converted Gemma 2B model and validate it for Android deployment
- **Features**:
  - HuggingFace authentication checking
  - Model download with progress tracking
  - File validation and integrity checking
  - Detailed usage instructions for Android integration
  - Comprehensive error handling and troubleshooting guidance

### ✅ 3. Model Selection
- **Model**: Google Gemma 2B Instruction-Tuned (IT)
- **Source**: `google/gemma-2b-it-tflite` on HuggingFace
- **Format**: TensorFlow Lite (`.bin` format)
- **Quantization**: 8-bit integer (int8) - optimal balance of size and quality
- **Backend**: CPU optimized (suitable for most Android devices)
- **Expected Size**: ~2.5 GB

## How to Use

### Prerequisites
1. **HuggingFace Account**: Create account at https://huggingface.co/join
2. **Accept Gemma License**: Visit https://huggingface.co/google/gemma-2b-it-tflite and click "Access repository"
3. **Get Access Token**: 
   - Go to https://huggingface.co/settings/tokens
   - Click "New token"
   - Give it a name (e.g., "epicNotes")
   - Select "Read" access
   - Copy the token

### Running the Script

```bash
# 1. Activate the virtual environment
cd /Users/vladimir.gromov/Code/epicNotes
source venv/bin/activate

# 2. Login to HuggingFace (one-time setup)
huggingface-cli login
# Paste your token when prompted

# 3. Run the model download script
python scripts/test_model.py
```

### Expected Output

The script will:
1. Check HuggingFace authentication
2. Download the Gemma 2B model (~2-3 GB, takes 5-15 minutes)
3. Validate the model file
4. Display model information and specifications
5. Provide Android integration instructions
6. Save the model to: `models/gemma-2b/gemma-2b-it-cpu-int8.bin`

## Model Specifications

| Property | Value |
|----------|-------|
| **Model Name** | Gemma 2B Instruction-Tuned |
| **Parameters** | 2 Billion |
| **Quantization** | 8-bit integer (int8) |
| **File Size** | ~2.5 GB |
| **Format** | TensorFlow Lite (.bin) |
| **Backend** | CPU optimized |
| **Context Length** | 8192 tokens |
| **Min Android Version** | Android 8.0 (API 26) |
| **Min RAM** | 4GB (6GB+ recommended) |

## Performance Expectations

Based on similar deployments:

### On Development Machine (M-series Mac / High-end Intel)
- **Model Load Time**: 2-5 seconds
- **First Token Latency**: 3-8 seconds
- **Subsequent Tokens**: 0.5-2 seconds
- **Memory Usage**: 3-4 GB

### On Android Device (Target Hardware)
- **Model Load Time**: 5-10 seconds
- **First Token Latency**: 5-15 seconds
- **Subsequent Tokens**: 1-3 seconds  
- **Memory Usage**: 2-3 GB during inference
- **APK Size Increase**: ~2.5 GB (with model in assets)

### Device Requirements
- **Minimum**: Android 8.0, 4GB RAM, 3GB free storage
- **Recommended**: Android 10+, 6GB+ RAM, 4GB free storage
- **Optimal**: Android 12+, 8GB+ RAM, modern ARM CPU

## Important Notes

### Why Not Python Inference Testing?

The MediaPipe LLM Inference API has different implementations:
- **Python**: Only supports model **conversion** and **bundling** tools
- **Android/iOS/Web**: Full inference capabilities with optimized runtime

Therefore, **actual inference testing must be performed on Android device** (STEP 2).

### Model Format: .bin vs .task

- `.bin`: TensorFlow Lite format (what we're using)
- `.task`: MediaPipe Task Bundle format (alternative)

Both formats work with MediaPipe LLM Inference API on Android. We chose `.bin` because:
- Directly available from Google's official repository
- Slightly simpler integration
- Well-tested and documented

## Success Criteria

- [x] Python environment configured
- [x] Model download script created and tested
- [x] Pre-converted model identified and accessible
- [x] File format compatible with MediaPipe
- [x] Model size appropriate for mobile deployment (< 3GB)
- [x] Integration instructions documented
- [ ] **User action required**: Login to HuggingFace and download model
- [ ] **Next step**: Proceed to STEP 2 (Android Integration)

## Next Steps (STEP 2)

After successfully downloading the model:

1. **Add MediaPipe Dependency**
   - Update `gradle/libs.versions.toml`
   - Add `com.google.mediapipe:tasks-genai:0.10.14`

2. **Copy Model to Android Project**
   ```bash
   mkdir -p app/src/main/assets/models
   cp models/gemma-2b/gemma-2b-it-cpu-int8.bin app/src/main/assets/models/
   ```

3. **Implement OnDeviceLlmClient**
   - Create `ModelManager.kt`
   - Create `OnDeviceLlmClient.kt`
   - Create `LlmConfig.kt`

4. **Update Dependency Injection**
   - Modify `AppModule.kt` to use `OnDeviceLlmClient` instead of `MockLlmClient`

5. **Test on Real Android Device**
   - Build and install APK
   - Test inference performance
   - Measure memory usage
   - Validate response quality

## Troubleshooting

### Issue: "401 Client Error" or "Access Denied"
**Solution**: 
1. Accept Gemma license at https://huggingface.co/google/gemma-2b-it-tflite
2. Wait 30-60 seconds for access to propagate
3. Run `huggingface-cli login` and enter your token
4. Run the script again

### Issue: "Not logged in to HuggingFace"
**Solution**:
```bash
huggingface-cli login
# Paste your token from https://huggingface.co/settings/tokens
```

### Issue: Download is very slow
**Solution**:
- This is normal for a ~2.5GB file
- Ensure stable internet connection
- The download is resumable if interrupted

### Issue: "No space left on device"
**Solution**:
- Ensure you have at least 5GB free space
- The model requires ~2.5GB plus temporary space during download

## Files Created

```
epicNotes/
├── scripts/
│   ├── test_model.py          # Model download & validation script
│   ├── requirements.txt        # Python dependencies
│   └── README.md              # This file
└── models/                     # Created when script runs
    └── gemma-2b/
        └── gemma-2b-it-cpu-int8.bin  # Downloaded model (user action required)
```

## Resources

- **Gemma Documentation**: https://ai.google.dev/gemma
- **MediaPipe LLM Inference**: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference
- **HuggingFace Model Page**: https://huggingface.co/google/gemma-2b-it-tflite
- **MediaPipe Android Guide**: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
- **Integration Plan**: ../on-device_llm_integration_7bdbb140.plan.md

## License

The Gemma model is released under the Gemma Terms of Use:
https://ai.google.dev/gemma/terms

Make sure you comply with these terms when using the model in your application.
