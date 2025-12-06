[![License](https://img.shields.io/github/license/smallrye/smallrye-llm.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven](https://img.shields.io/maven-central/v/dev.langchain4j.cdi/langchain4j-cdi-parent?color=green)](https://central.sonatype.com/search?q=dev.langchain4j.cdi%3Alangchain4j-cdi-parent)

[![Build Status](https://img.shields.io/github/actions/workflow/status/langchain4j/langchain4j-cdi/main.yaml?branch=main&style=for-the-badge&label=CI%20BUILD&logo=github)](https://github.com/langchain4j/langchain4j/actions/workflows/main.yaml)
[![Nightly Build](https://img.shields.io/github/actions/workflow/status/langchain4j/langchain4j-cdi/nightly_jdk17.yaml?branch=main&style=for-the-badge&label=NIGHTLY%20BUILD&logo=github)](https://github.com/langchain4j/langchain4j/actions/workflows/nightly_jdk17.yaml)
[![CODACY](https://img.shields.io/badge/Codacy-Dashboard-blue?style=for-the-badge&logo=codacy)](https://app.codacy.com/gh/langchain4j/langchain4j/dashboard)

[![Discord](https://img.shields.io/discord/1156626270772269217?logoColor=violet)](https://discord.gg/JzTFvyjG6R)
[![BlueSky](https://img.shields.io/badge/@langchain4j-follow-blue?logo=bluesky&style=for-the-badge)](https://bsky.app/profile/langchain4j.dev)
[![X](https://img.shields.io/badge/@langchain4j-follow-blue?logo=x&style=for-the-badge)](https://x.com/langchain4j)
[![Maven Version](https://img.shields.io/maven-central/v/dev.langchain4j/langchain4j?logo=apachemaven&style=for-the-badge)](https://search.maven.org/#search|gav|1|g:"dev.langchain4j"%20AND%20a:"langchain4j")

# 🚀 Langchain4j integration with MicroProfile™ and Jakarta™ specifications

A comprehensive CDI (Contexts and Dependency Injection) extension that brings enterprise-grade AI capabilities to Jakarta EE and Eclipse MicroProfile applications through seamless LangChain4j integration.

## 📋 Overview

This project provides a powerful bridge between the LangChain4j AI framework and enterprise Java applications, enabling developers to inject AI services directly into their CDI-managed beans with full support for enterprise features like fault tolerance, telemetry, and configuration management.

### Key Features

- **🔌 Seamless Integration**: Native CDI extension for LangChain4j with `@RegisterAIService` annotation
- **🏢 Enterprise Ready**: Built-in support for MicroProfile Fault Tolerance, Config, and Telemetry
- **🎯 Multiple Deployment Models**: Support for both portable and build-compatible CDI extensions
- **🚀 Framework Agnostic**: Works with Quarkus, WildFly, Helidon, GlassFish, Liberty, Payara, and other Jakarta EE servers
- **🔧 Configuration Driven**: External configuration support through MicroProfile Config
- **📊 Observable**: Comprehensive telemetry and monitoring capabilities
- **🛡️ Resilient**: Built-in fault tolerance with retries, timeouts, and fallbacks

## 🏗️ Architecture

The project is structured into several modules, each serving a specific purpose:

### Core Modules

- **`langchain4j-cdi-core`**: Fundamental CDI integration classes and SPI definitions
- **`langchain4j-cdi-portable-ext`**: Portable CDI extension implementation for runtime service registration
- **`langchain4j-cdi-build-compatible-ext`**: Build-time CDI extension for ahead-of-time compilation scenarios
- **`langchain4j-cdi-mp`**: MicroProfile specific integration
  - **`langchain4j-cdi-config`**: MicroProfile Config integration for external configuration
  - **`langchain4j-cdi-fault-tolerance`**: MicroProfile Fault Tolerance integration for resilient AI services
  - **`langchain4j-cdi-telemetry`**: MicroProfile Telemetry integration for observability and monitoring

## 🚀 Quick Start

### 1. Add Dependencies

Add the required dependencies to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.langchain4j.cdi</groupId>
    <artifactId>langchain4j-cdi-portable-ext</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j.cdi</groupId>
    <artifactId>langchain4j-cdi-config</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Define an AI Service

Create an AI service interface annotated with `@RegisterAIService`:

```java
@RegisterAIService(
    tools = BookingService.class,
    chatMemoryName = "chat-memory"
)
public interface ChatAiService {
    
    @SystemMessage("You are a helpful customer service assistant.")
    @Timeout(unit = ChronoUnit.MINUTES, value = 5)
    @Retry(maxRetries = 2)
    @Fallback(fallbackMethod = "chatFallback")
    String chat(String userMessage);
    
    default String chatFallback(String userMessage) {
        return "I'm temporarily unavailable. Please try again later.";
    }
}
```

### 3. Inject and Use

Inject the AI service into your CDI beans:

```java
@ApplicationScoped
@Path("/car-booking")
public class ChatResource {
    
    @Inject
    ChatAiService chatService;
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/chat")
    public String chat(String message) {
        return chatService.chat(message);
    }
}
```

### 4. Configure Models

Configure your AI models through `microprofile-config.properties`:

```properties
# Chat model configuration
langchain4j.chat-model.provider=ollama
langchain4j.chat-model.ollama.base-url=http://localhost:11434
langchain4j.chat-model.ollama.model-name=llama3.1

# Memory configuration
langchain4j.chat-memory.chat-memory.max-messages=100
```

## 📖 Examples

The project includes comprehensive examples for various Jakarta EE servers:

- **Quarkus**: Native LangChain4j integration with Quarkus-specific optimizations
- **Helidon**: Both portable extension and standard LangChain4j usage examples
- **GlassFish**: Full Jakarta EE server implementation
- **Liberty**: IBM WebSphere Liberty integration
- **Payara**: Payara Server implementation

Each example demonstrates a car booking application with:
- **Chat Service**: Natural language customer support with RAG (Retrieval Augmented Generation)
- **Fraud Detection**: AI-powered fraud detection service
- **Function Calling**: Integration with business logic through tool calling

## 🛠️ How to run examples

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

## 🎯 Use Cases

This integration is perfect for enterprise applications that need:

- **Customer Support Chatbots**: AI-powered customer service with access to business data
- **Document Analysis**: RAG-enabled document processing and question answering
- **Fraud Detection**: AI-based risk assessment and fraud prevention
- **Content Generation**: Automated content creation with business context
- **Decision Support**: AI-assisted business decision making
- **Process Automation**: Intelligent workflow automation with natural language interfaces

## 🔧 Configuration

The CDI extension supports extensive configuration through MicroProfile Config:

### Chat Models
```properties
# Ollama configuration
langchain4j.chat-model.provider=ollama
langchain4j.chat-model.ollama.base-url=http://localhost:11434
langchain4j.chat-model.ollama.model-name=llama3.1
langchain4j.chat-model.ollama.timeout=60s

# OpenAI configuration
langchain4j.chat-model.provider=openai
langchain4j.chat-model.openai.api-key=${OPENAI_API_KEY}
langchain4j.chat-model.openai.model-name=gpt-4
```

### Memory Configuration
```properties
langchain4j.chat-memory.default.max-messages=100
langchain4j.chat-memory.default.type=token-window
```

### Embedding Models
```properties
langchain4j.embedding-model.provider=ollama
langchain4j.embedding-model.ollama.model-name=nomic-embed-text
```

## 🔍 Advanced Features

### Fault Tolerance Integration

Leverage MicroProfile Fault Tolerance annotations:

```java
@RegisterAIService
public interface ResilientAiService {
    
    @Retry(maxRetries = 3, delay = 1000)
    @Timeout(5000)
    @CircuitBreaker(requestVolumeThreshold = 10)
    @Fallback(fallbackMethod = "handleFailure")
    String processRequest(String input);
}
```

### Telemetry and Monitoring

Automatic telemetry integration provides metrics for:
- Request/response times
- Success/failure rates  
- Token usage
- Model performance

### Custom Tools and Functions

Define business functions that AI can call:

```java
@ApplicationScoped
public class BookingTools {
    
    @Tool("Cancel a booking by ID")
    public String cancelBooking(@P("booking ID") String bookingId) {
        // Business logic here
        return "Booking " + bookingId + " cancelled successfully";
    }
}
```

## 🤝 Contributing

If you want to contribute, please have a look at [CONTRIBUTING.md](CONTRIBUTING.md).

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🌟 Getting Started

Ready to integrate AI into your enterprise Java application? 

1. **Explore the examples**: Start with the framework-specific examples in the `examples/` directory
2. **Read the documentation**: Check out individual module documentation for detailed configuration options
3. **Join the community**: Connect with other developers using LangChain4j CDI integration
4. **Contribute**: Help improve the project by reporting issues or submitting pull requests

---

**Built with ❤️ by the LangChain4j CDI Community**
