#!/usr/bin/env bash
# start-ngrok.sh
# Starts ngrok for local port 8080 and prints the public HTTPS URL

if ! command -v ngrok >/dev/null 2>&1; then
  echo "ngrok not found. Install from https://ngrok.com and ensure 'ngrok' is on your PATH."
  exit 1
fi

# Start ngrok in background if not running
if ! curl --silent http://127.0.0.1:4040/api/tunnels >/dev/null 2>&1; then
  echo "Starting ngrok on port 8080..."
  ngrok http 8080 >/dev/null 2>&1 &
  sleep 2
fi

# Query ngrok API for public https URL
TUNNELS_JSON=$(curl --silent http://127.0.0.1:4040/api/tunnels)
NGROK_URL=$(echo "$TUNNELS_JSON" | jq -r '.tunnels[] | select(.proto=="https") | .public_url' | head -n1)
if [ -z "$NGROK_URL" ]; then
  NGROK_URL=$(echo "$TUNNELS_JSON" | jq -r '.tunnels[0].public_url')
fi
if [ -z "$NGROK_URL" ]; then
  echo "Failed to get ngrok public URL. Raw response:"
  echo "$TUNNELS_JSON"
  exit 1
fi

echo "ngrok public URL: $NGROK_URL"
# copy to clipboard if possible
if command -v pbcopy >/dev/null 2>&1; then
  echo -n "$NGROK_URL" | pbcopy && echo "(Copied to clipboard)"
elif command -v xclip >/dev/null 2>&1; then
  echo -n "$NGROK_URL" | xclip -selection clipboard && echo "(Copied to clipboard)"
fi

