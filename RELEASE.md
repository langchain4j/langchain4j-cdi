# How to Release

## Prerequisites

Configure these repository secrets:
- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD` 
- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`

## Snapshot Release

Automatic on every push to `main`, or manually:
1. Go to Actions → "LangChain4J CDI Snapshot Release"
2. Click "Run workflow" → Select `main` branch → "Run workflow"

## Official Release

1. Go to Actions → "Publish a new release"
2. Click "Run workflow"
3. Enter:
   - **Release version**: e.g., `1.2.0`
   - **Next version**: e.g., `1.3.0` (snapshot suffix added automatically)
4. Click "Run workflow"

The workflow will:
- Build and test the code
- Release to Maven Central
- Create a GitHub release with auto-generated notes