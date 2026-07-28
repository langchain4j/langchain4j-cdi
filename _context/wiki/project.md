# Project Overview

## What is langchain4j-cdi?

langchain4j-cdi provides dependency injection support for LangChain4j in enterprise Java environments. It enables seamless integration of AI capabilities into Jakarta EE and CDI-based applications through standard dependency injection patterns.

## Main Goals and Objectives

The primary focus of this project is **ease of use and developer experience**. The goal is to make it simple for Java developers to integrate LangChain4j AI capabilities into their enterprise applications using familiar CDI patterns and conventions.

Key objectives include:
- Providing intuitive CDI integration for LangChain4j
- Ensuring smooth developer experience with minimal configuration
- Supporting standard Jakarta EE and CDI patterns
- Maintaining high code quality and comprehensive documentation

## Key Stakeholders and Users

- **Enterprise Java Developers**: Primary users who want to add AI capabilities to their Jakarta EE applications
- **CDI Runtime Providers**: Various CDI implementations and runtimes that need to support this integration
- **LangChain4j Community**: Broader ecosystem of developers using LangChain4j in Java

## Important Modules and Components

### Core Architecture

The project is built around **portable-ext for CDI integration patterns**, which serves as the foundation for the CDI integration approach.

Key modules include:

- **langchain4j-cdi-core**: Foundation module providing core CDI integration
- **langchain4j-cdi-portable-ext**: Primary focus - implements portable CDI extension patterns
- **langchain4j-cdi-build-compatible-ext**: Build-time compatible extensions
- **langchain4j-cdi-mcp**: Model Context Protocol integration (newer feature)
- **langchain4j-cdi-a2a**: Agent-to-Agent communication support (newer feature)
- **langchain4j-cdi-mp**: MicroProfile integrations (config, fault tolerance, telemetry)
- **langchain4j-cdi-el**: Expression Language support

### Integration and Examples

- **langchain4j-cdi-integration-tests**: Comprehensive test suites for various runtimes
  - Helidon
  - Jakarta EE (WildFly, OpenLiberty, Payara)
  - Quarkus
- **examples/**: Real-world examples demonstrating usage across different application servers

## Development Workflow

The project follows standard Maven-based Java development with multiple runtime targets. Testing is performed across various CDI implementations to ensure broad compatibility.

## Important Considerations

- Focus on **portable CDI extension patterns** as the primary integration mechanism
- Maintain compatibility across multiple CDI runtimes and Jakarta EE implementations
- Prioritize developer experience in API design and documentation
- Ensure comprehensive test coverage across supported platforms
