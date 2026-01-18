# GitHub Actions + Telegram Integration Setup

This workflow automatically builds APK files and sends them to a Telegram channel.

## Setup Instructions

### 1. Create a Telegram Bot

1. Open Telegram and search for [@BotFather](https://t.me/BotFather)
2. Send `/newbot` command
3. Follow the instructions to create your bot
4. Copy the API token (looks like: `123456789:ABCdefGHIjklMNOpqrsTUVwxyz`)
5. Add this bot to your channel/group and make it an administrator

### 2. Get Your Chat ID

**For a channel:**
1. Create a public channel or make your existing channel public (starts with `@`)
2. The chat ID will be `@your_channel_name`

**For a private channel or group:**
1. Add your bot to the channel/group
2. Get an update from the bot using:
   ```bash
   curl https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates
   ```
3. Look for the `chat_id` field in the response

**For your personal ID:**
1. Start a chat with your bot
2. Send a message to the bot
3. Get updates:
   ```bash
   curl https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates
   ```
4. Copy the `id` field from the result

### 3. Add Secrets to GitHub

1. Go to your repository on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add these secrets:

   - **TELEGRAM_BOT_TOKEN**: Your bot's API token from BotFather
   - **TELEGRAM_CHAT_ID**: The chat/channel ID where you want to receive APKs

## Workflow Triggers

The workflow runs on:
- Push to `master` or `main` branch
- Pull requests to `master` or `main` branch
- Manual trigger (via GitHub Actions UI)

**Note:** Telegram notifications are only sent on pushes to the `master` branch, not on pull requests.

## What Gets Built

- **Debug APK**: `app-debug.apk` - Unsigned debug build
- **Release APK**: `app-release-unsigned.apk` - Unsigned release build

## Artifacts

Both APK files are also uploaded as GitHub Actions artifacts with a 30-day retention period.

## Manual Trigger

To manually trigger the workflow:
1. Go to **Actions** tab in your repository
2. Select "Build APK and Send to Telegram" workflow
3. Click **Run workflow**
4. Choose the branch and click **Run workflow**

## Troubleshooting

### Bot doesn't send messages
- Make sure the bot is added to the channel as an **administrator**
- Verify the bot has permission to send messages
- Check that `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID` secrets are correct

### Build fails
- Check the logs in GitHub Actions for specific errors
- Ensure the project builds locally with `./gradlew assembleDebug`
