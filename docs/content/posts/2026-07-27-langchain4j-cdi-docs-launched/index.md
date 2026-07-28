---
layout: post
title: 'LangChain4j CDI Documentation Site Launched'
date: 2026-07-27
tags: docs langchain4j cdi
synopsis: 'The LangChain4j CDI project now has a dedicated documentation website built with Roq.'
author: ehsavoie
---

We are excited to announce the launch of the official LangChain4j CDI documentation website!

## What is LangChain4j CDI?

LangChain4j CDI is a CDI extension that integrates the [LangChain4j](https://docs.langchain4j.dev/) AI framework with Jakarta EE and Eclipse MicroProfile applications. It enables developers to inject AI services into CDI-managed beans with enterprise features.

## Key Features

- **AI Service Injection** -- Declare AI service interfaces with `@RegisterAIService` and inject them via CDI.
- **11 Agent Topologies** -- Sequence, loop, parallel, conditional, supervisor, planner, A2A, MCP client, and more.
- **MCP Server** -- Turn CDI beans into Model Context Protocol servers.
- **MicroProfile Integration** -- Configuration, fault tolerance, and telemetry.

## Getting Started

Head over to the [Getting Started]({site.url('getting-started')}) guide to begin using LangChain4j CDI in your project.

Check out the [Examples]({site.url('examples')}) page for sample applications running on Quarkus, WildFly, Helidon, GlassFish, and Open Liberty.
