package dev.langchain4j.cdi.mcp.portableextension;

import java.lang.reflect.Method;
import java.util.List;

record McpToolCandidate(
        Class<?> beanClass,
        List<Method> toolMethods,
        List<Method> resourceMethods,
        List<Method> resourceTemplateMethods,
        List<Method> promptMethods) {}
