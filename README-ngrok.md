# Using ngrok to expose your local backend (quick dev setup)

This repo includes helper scripts to run ngrok and get a public HTTPS URL for your local backend so any device (phone, tablet) can connect to it without firewall or port-binding issues.

Why use ngrok?
- Avoid changing firewall or server binding.
- Provides a secure public HTTPS URL for quick testing from any device.
- Useful for mobile testing and webhook development.

Files added
- `scripts/start-ngrok.bat` — Windows helper. Starts `ngrok http 8080` and copies the public URL to clipboard.
- `scripts/start-ngrok.sh` — macOS/Linux helper (requires `jq` for JSON parsing).

Pre-requisites
- Install ngrok: https://ngrok.com
  - Download and place `ngrok` (or ngrok.exe on Windows) on your PATH or in the `scripts/` folder.
- (macOS/Linux) Install `jq` for JSON parsing: `brew install jq` or `sudo apt install jq`.

Windows quick start
1. Open PowerShell and run the helper script:

   ```powershell
   cd C:\Users\shubh\AndroidStudioProjects\Khata4U\scripts
   .\start-ngrok.bat
   ```

2. The script will start `ngrok` (if not already running) and will copy the public https URL to your clipboard and display it. Paste this URL into the app's "Edit API" dialog and Save.
3. In the app tap "Test connection" — it should now connect via the public ngrok URL.

macOS / Linux quick start
1. Open a terminal and run:

   ```bash
   cd /path/to/Khata4U/scripts
   ./start-ngrok.sh
   ```

2. The script will print the public https URL and copy it to the clipboard if `pbcopy` or `xclip` is installed. Paste it into the app's "Edit API" dialog and Save.
3. In the app tap "Test connection" — it should now connect via the public ngrok URL.

Notes & security
- ngrok is only for development/testing. Do not use the public URL in production.
- If your backend requires host headers or specific hostnames, you might need to adjust server-side checks to accept the ngrok host or configure ngrok with hostname mapping (paid plan).

If you want, I can:
- Add a small UI in the app that pings the local ngrok API to automatically fetch the public URL (if the phone is on the same network as the PC) or guide users to paste it.
- Add an optional script that launches `ngrok` and automatically updates a debug file the app can read to pick the URL.


