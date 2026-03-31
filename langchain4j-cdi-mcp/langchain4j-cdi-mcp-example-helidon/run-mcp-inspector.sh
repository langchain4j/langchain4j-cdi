#!/bin/sh
# Launches the MCP Inspector UI against the local Helidon MCP server.
# Prerequisites: Node.js / npm must be installed.
#
# 1. Start the Helidon server first:
#      java -jar target/helidon-mcp-server.jar
#
# 2. Then run this script:
#      ./run-mcp-inspector.sh

npx @modelcontextprotocol/inspector --url http://localhost:8080/mcp
