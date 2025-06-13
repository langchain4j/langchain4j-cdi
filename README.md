[![SmallRye Build](https://github.com/smallrye/smallrye-llm/workflows/SmallRye%20Build/badge.svg?branch=main)](https://github.com/smallrye/smallrye-llm/actions?query=workflow%3A%22SmallRye+Build%22)
[![License](https://img.shields.io/github/license/smallrye/smallrye-llm.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven](https://img.shields.io/maven-central/v/dev.langchain4j.cdi/langchain4j-cdi-parent?color=green)](https://central.sonatype.com/search?q=dev.langchain4j.cdi%3Alangchain4j-cdi-parent)

[![Build Status](https://img.shields.io/github/actions/workflow/status/langchain4j/langchain4j/main.yaml?branch=main&style=for-the-badge&label=CI%20BUILD&logo=github)](https://github.com/langchain4j/langchain4j/actions/workflows/main.yaml)
[![Nightly Build](https://img.shields.io/github/actions/workflow/status/langchain4j/langchain4j/nightly_jdk17.yaml?branch=main&style=for-the-badge&label=NIGHTLY%20BUILD&logo=github)](https://github.com/langchain4j/langchain4j/actions/workflows/nightly_jdk17.yaml)
[![CODACY](https://img.shields.io/badge/Codacy-Dashboard-blue?style=for-the-badge&logo=codacy)](https://app.codacy.com/gh/langchain4j/langchain4j/dashboard)

[![Discord](https://dcbadge.vercel.app/api/server/JzTFvyjG6R?style=for-the-badge)](https://discord.gg/JzTFvyjG6R)
[![BlueSky](https://img.shields.io/badge/@langchain4j-follow-blue?logo=bluesky&style=for-the-badge)](https://bsky.app/profile/langchain4j.dev)
[![X](https://img.shields.io/badge/@langchain4j-follow-blue?logo=x&style=for-the-badge)](https://x.com/langchain4j)
[![Maven Version](https://img.shields.io/maven-central/v/dev.langchain4j/langchain4j?logo=apachemaven&style=for-the-badge)](https://search.maven.org/#search|gav|1|g:"dev.langchain4j"%20AND%20a:"langchain4j")

# 🚀 Langchain4j integration with MicroProfile™ and Jakarta™ specifications

Experimentation around LLM and CDI (JakartaEE and Eclipse Microprofile)

## How to run examples

### Use LM Studio

#### Install LM Studio

https://lmstudio.ai/

#### Download model

Mistral 7B Instruct v0.2

#### Run

On left goto "local server", select the model in dropdown combo on the top, then start server

### Use Ollama

Running Ollama with the llama3.1 model:

```bash
CONTAINER_ENGINE=$(command -v podman || command -v docker)
$CONTAINER_ENGINE run -d --rm --name ollama --replace --pull=always -p 11434:11434 -v ollama:/root/.ollama --stop-signal=SIGKILL docker.io/ollama/ollama
$CONTAINER_ENGINE exec -it ollama ollama run llama3.1
```

### Run the examples

Go to each example README.md to see how to execute the example.

## Contributing

If you want to contribute, please have a look at [CONTRIBUTING.md](CONTRIBUTING.md).

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.