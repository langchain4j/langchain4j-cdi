#!/bin/bash
# Source the common Ollama setup script
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
OLLAMA_MODEL="qwen3.5:4b"
source "$SCRIPT_DIR/../ollama-setup.sh"

# Function to cleanup on exit
cleanup() {
    echo "Cleaning up..."
    cleanup_ollama
    exit 0
}

# Set up trap to catch SIGINT (Ctrl+C) and SIGTERM
trap cleanup SIGINT SIGTERM

# Setup Ollama
setup_ollama

mvn clean package
$SCRIPT_DIR/target/server/bin/standalone.sh --debug