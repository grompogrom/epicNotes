# Quick Start: Debug LLM App in 3 Steps

## The Problem

Your app logs show **no LLM activity** - no initialization, no errors, nothing. This typically means:
- Model hasn't been downloaded (2GB file)
- LLM hasn't been used yet (no message sent)
- Or initialization failed silently

## Fix in 3 Steps

### Step 1: Run Diagnostic (2 minutes)

```bash
cd /Users/vladimir.gromov/Code/epicNotes
./diagnose_llm.sh
```

This checks:
- ✓ Device connected
- ✓ App installed
- ✓ CPU compatible (ARM64)
- ✓ RAM sufficient (4GB+)
- ✓ Storage available (2GB+)
- ✓ **Model file exists** ← Most important!
- ✓ Native libraries present
- ✓ No recent errors

**Expected output** if working:
```
✓ Device connected
✓ App installed
✓ arm64-v8a supported
✓ Sufficient RAM (6GB)
✓ Sufficient storage (15GB free)
✓ Model file found
✓ Model size looks correct (2048MB)
✓ All native libraries present
✓ First start completed
✓ No recent errors
✓ Using LocalLlamaLlmClient

✓ All critical checks passed!
```

**Common issue** found:
```
✗ Model file not found
  Expected path: /data/data/com.epicnotes.chat/files/models/llama-3.2-3b-instruct-q4_k_m.gguf
  The app should download it on first launch
```

### Step 2: Test the App (1 minute)

1. **Clear old logs**:
   ```bash
   adb logcat -c
   ```

2. **Start log monitoring** (in a new terminal):
   ```bash
   adb logcat | grep -E "ChatApp|Llama|Model|ChatViewModel"
   ```

3. **Open the app** on your device

4. **Send a test message**: Type "Hello" and press Send

5. **Watch the logs** for 30 seconds

### Step 3: Interpret Results (1 minute)

#### Scenario A: Model Downloading

**Logs show:**
```
ChatApp: First start detected, checking model...
ModelManager: Model not found, starting download...
ModelManager: Starting download of 2048MB
ModelManager: Download progress: 5%
ModelManager: Download progress: 10%
...
```

**What this means:** ✅ Everything is working! Just wait for download (10-30 min)

**What to do:** Wait for download to complete, then retry sending a message.

---

#### Scenario B: Model Loading

**Logs show:**
```
LocalLlamaLlmClient: Initializing LocalLlamaLlmClient...
LocalLlamaLlmClient: Model not ready, downloading...
LocalLlamaLlmClient: Model path obtained: /data/data/.../llama-3.2...
LlamaJni: Loading model...
LocalLlamaLlmClient: Initialization complete
```

**What this means:** ✅ LLM initializing successfully!

**What to do:** Wait for response (can take 30-60 seconds for first message).

---

#### Scenario C: Response Generated

**Logs show:**
```
ChatViewModel: Sending message...
LocalLlamaLlmClient: replyTo called with 1 messages
LocalLlamaLlmClient: Prompt length: 234 characters
LocalLlamaLlmClient: Generated response: Hello! How can I help...
LocalLlamaLlmClient: Response length: 156 characters
ChatViewModel: Message sent successfully
```

**What this means:** ✅ **Everything works perfectly!**

**What to do:** Enjoy your on-device AI assistant!

---

#### Scenario D: No Logs at All

**Logs show:** (nothing)

**What this means:** ❌ LLM not being used

**Possible causes:**
1. Using MockLlmClient instead of LocalLlamaLlmClient
2. App not actually sending messages (UI issue)
3. Logs filtered out

**What to do:**

Check which client is active:
```bash
grep "useLocalLlama" app/src/main/java/com/epicnotes/chat/presentation/di/AppModule.kt
```

Should show: `single(qualifier = named("useLocalLlama")) { true }`

If it's `false`, change to `true` and rebuild:
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

#### Scenario E: Error Messages

**Logs show:**
```
LlamaJni: Failed to load model: /data/data/.../llama-3.2...
LocalLlamaLlmClient: Failed to initialize: Model not found
```

**What this means:** ❌ Model file missing or corrupted

**What to do:** Manual model download:

```bash
# 1. Download model (2GB) - takes 5-15 minutes
wget https://huggingface.co/MaziyarPanahi/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct.Q4_K_M.gguf

# 2. Push to device
adb push Llama-3.2-3B-Instruct.Q4_K_M.gguf /data/local/tmp/

# 3. Move to app directory
adb shell run-as com.epicnotes.chat mkdir -p /data/data/com.epicnotes.chat/files/models

adb shell run-as com.epicnotes.chat cp /data/local/tmp/Llama-3.2-3B-Instruct.Q4_K_M.gguf /data/data/com.epicnotes.chat/files/models/llama-3.2-3b-instruct-q4_k_m.gguf

# 4. Verify
adb shell run-as com.epicnotes.chat ls -lh /data/data/com.epicnotes.chat/files/models/
# Should show: llama-3.2-3b-instruct-q4_k_m.gguf (2048MB)

# 5. Restart app
adb shell am force-stop com.epicnotes.chat
adb shell am start -n com.epicnotes.chat/.MainActivity
```

---

## Visual Debug Flowchart

```
Start
  │
  ├─► Run ./diagnose_llm.sh
  │       │
  │       ├─► All checks pass? ───────► Go to "Test App"
  │       │
  │       └─► Model missing? ──────────► Manual download (see Scenario E)
  │
  ├─► Open app + Send "Hello"
  │       │
  │       ├─► See download logs? ─────► Wait 10-30 min ───► Retry
  │       │
  │       ├─► See initialization? ────► Wait 30-60 sec ──► Success!
  │       │
  │       ├─► See response logs? ─────► **Working!** ✓
  │       │
  │       ├─► No logs at all? ────────► Check useLocalLlama=true
  │       │
  │       └─► Error messages? ────────► See DEBUG_GUIDE.md
  │
  └─► Still stuck? ───────────────────► Run full diagnostics:
                                         adb logcat -v threadtime > /tmp/full_log.txt
                                         Share first 1000 relevant lines
```

## Key Files

- **`diagnose_llm.sh`**: Automated system check (run this first!)
- **`DEBUG_GUIDE.md`**: Comprehensive troubleshooting guide (50+ solutions)
- **`DEBUGGING_SUMMARY.md`**: Analysis of your current logs + fixes
- **`QUICK_START_DEBUG.md`**: This file (3-step quick fix)

## Most Common Issues (95% of problems)

### 1. Model Not Downloaded (80%)
**Symptom:** Logs show "Model not found" or no logs at all  
**Fix:** Wait for auto-download or use manual download (Scenario E)  
**Time:** 10-30 minutes

### 2. Not Actually Testing (10%)
**Symptom:** No logs appear  
**Fix:** Make sure to actually send a message in the app  
**Time:** 1 minute

### 3. Using Mock Client (5%)
**Symptom:** No LLM logs, but app shows instant responses  
**Fix:** Set `useLocalLlama = true` in AppModule.kt  
**Time:** 2 minutes + rebuild

### 4. Insufficient Resources (3%)
**Symptom:** Logs show "Failed to load model" or crashes  
**Fix:** Need 4GB+ RAM, ARM64 CPU, 2GB+ storage  
**Time:** Upgrade device or use smaller model

### 5. Native Library Issue (2%)
**Symptom:** "UnsatisfiedLinkError" or "library not found"  
**Fix:** `./gradlew clean assembleDebug`  
**Time:** 5 minutes

## Checklist

Before asking for help, verify:

- [ ] Ran `./diagnose_llm.sh` (all checks pass)
- [ ] Model file exists (2048MB)
- [ ] Device has ARM64 CPU
- [ ] Device has 4GB+ RAM
- [ ] Device has 2GB+ free storage
- [ ] Set `useLocalLlama = true` in AppModule.kt
- [ ] Actually sent a message in the app
- [ ] Waited 30-60 seconds for response
- [ ] Checked logs: `adb logcat | grep -E "ChatApp|Llama"`

## Expected Timings

| Action | First Time | Subsequent Times |
|--------|-----------|------------------|
| App launch | 1-2 sec | 1-2 sec |
| Model download | 10-30 min | N/A (cached) |
| Model loading | 10-20 sec | 10-20 sec |
| Send message | 5-30 sec | 5-30 sec |
| Generate response | 30-90 sec | 30-90 sec |

**Total first message**: 1-2 minutes (if model already downloaded)  
**Total subsequent messages**: 30-90 seconds

## Still Need Help?

1. Run full diagnostic:
   ```bash
   ./diagnose_llm.sh > /tmp/diagnostic_report.txt
   ```

2. Capture test log:
   ```bash
   adb logcat -c
   adb logcat > /tmp/test_log.txt &
   # Send message in app
   sleep 120
   killall adb
   ```

3. Share:
   - `/tmp/diagnostic_report.txt`
   - First 500 lines of `/tmp/test_log.txt` that mention your app
   - Device model and Android version
   - What you see in the app UI when you send a message

## Success Looks Like

When everything works, you'll see:

**In logs:**
```
I ChatApp: Initializing...
I ModelManager: Model is already present
I LocalLlamaLlmClient: Initializing LocalLlamaLlmClient...
I LocalLlamaLlmClient: Loading model from: ...
I LlamaJni: Model loaded successfully
I LocalLlamaLlmClient: LocalLlamaLlmClient initialized successfully
I ChatViewModel: Sending message...
I LocalLlamaLlmClient: replyTo called with 1 messages
I LocalLlamaLlmClient: Generated response: Hello! I'm your AI assistant...
I ChatViewModel: Message sent successfully
```

**In app UI:**
- Your message appears instantly
- "Thinking..." indicator shows
- AI response appears after 30-60 seconds
- Can continue conversation

**Phone behavior:**
- Might warm up during inference (normal)
- Battery drains faster during active use (normal)
- Response time improves after first message (model cached)

---

**TL;DR**: Run `./diagnose_llm.sh`, send a message, check if model is downloading. That's it!
