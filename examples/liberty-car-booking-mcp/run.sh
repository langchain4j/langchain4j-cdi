#!/bin/bash
# Generated with help of Junie

# Source the common Ollama setup script
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
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

# Run the application
echo "Starting the application..."
mvn liberty:dev

# Cleanup will be handled by the trap
