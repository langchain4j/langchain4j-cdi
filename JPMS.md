# JPMS (Java Platform Module System) support

This project ships explicit Java module descriptors so its artifacts can be consumed on the **module path** and
assembled into a **custom modular runtime image** (jlink) by downstream consumers such as *vidocq*.

## Module names

Every published jar carries a stable module name. Ten of the eleven jars ship an explicit `module-info.java`
(declared as `open module` so the CDI container — Weld and friends — can reflect over and proxy the beans). One jar
(`langchain4j-cdi-a2a`) can only offer a stable `Automatic-Module-Name` for now (see [Known limitations](#known-limitations)).

| Maven artifact | Module name | Kind |
|----------------|-------------|------|
| `langchain4j-cdi-core` | `dev.langchain4j.cdi.core` | explicit (open) |
| `langchain4j-cdi-portable-ext` | `dev.langchain4j.cdi.portable` | explicit (open) |
| `langchain4j-cdi-build-compatible-ext` | `dev.langchain4j.cdi.buildcompatible` | explicit (open) |
| `langchain4j-cdi-el` | `dev.langchain4j.cdi.el` | explicit (open) |
| `langchain4j-cdi-a2a` | `dev.langchain4j.cdi.a2a` | **automatic** (Automatic-Module-Name) |
| `langchain4j-cdi-config` | `dev.langchain4j.cdi.mp.config` | explicit (open) |
| `langchain4j-cdi-fault-tolerance` | `dev.langchain4j.cdi.mp.faulttolerance` | explicit (open) |
| `langchain4j-cdi-telemetry` | `dev.langchain4j.cdi.mp.telemetry` | explicit (open) |
| `langchain4j-cdi-mcp-server` | `dev.langchain4j.cdi.mcp.server` | explicit (open) |
| `langchain4j-cdi-mcp-portable-ext` | `dev.langchain4j.cdi.mcp.portable` | explicit (open) |
| `langchain4j-cdi-mcp-build-compatible-ext` | `dev.langchain4j.cdi.mcp.buildcompatible` | explicit (open) |

The SPI providers are declared with `provides ... with ...` in the descriptors **and** kept as `META-INF/services`
files, so discovery works on both the module path and the class path.

## Consuming on the module path

A modular consumer requires the modules it uses and declares `uses` for any SPI it looks up:

```java
module com.example.app {
    requires dev.langchain4j.cdi.core;
    requires dev.langchain4j.cdi.el;          // #{...} expression resolver
    requires dev.langchain4j.cdi.mp.config;   // ${...} MicroProfile Config resolver

    // Only if your code calls ServiceLoader for these SPIs directly:
    uses dev.langchain4j.cdi.spi.ExpressionResolver;
    uses dev.langchain4j.cdi.agent.spi.A2AAgentBuilder;
    uses dev.langchain4j.cdi.core.config.spi.LLMConfig;
}
```

Notes:

- `langchain4j-agentic` is an **optional** dependency (agent topologies). In JPMS it is declared `requires static`
  (compile-time, optional at runtime). If you use the agent API, add `requires static langchain4j.agentic;` and put
  the jar on your module path.
- Jakarta APIs are `provided`: the target runtime (application server, or your jlink image) must supply
  `jakarta.cdi`, `jakarta.interceptor`, `jakarta.annotation`, `jakarta.el`, `jakarta.ws.rs`, `jakarta.json`,
  `jakarta.json.bind`, etc.
- The upstream LangChain4j and MCP-Java jars are not modular yet; on a plain module path they resolve as automatic
  modules (`langchain4j`, `langchain4j.core`, `langchain4j.agentic`, `langchain4j.http.client`, `mcp.annotations`,
  `mcp.model`, `mcp.server.api`). For a **jlink** image they must be patched — see below.

## Breaking change (SPI package move)

To make the modules JPMS-legal, the split package shared by `langchain4j-cdi-core` and
`langchain4j-cdi-build-compatible-ext` was removed by moving the build-compatible classes into
`dev.langchain4j.cdi.buildcompatible.*`. This includes the **public SPI**:

```
dev.langchain4j.cdi.spi.AISyntheticBeanCreatorClassFactory
        →  dev.langchain4j.cdi.buildcompatible.spi.AISyntheticBeanCreatorClassFactory
```

Downstream implementers of this SPI (for example the WildFly integration) must:

1. update their `implements` / `import` to the new package, and
2. rename their service file to
   `META-INF/services/dev.langchain4j.cdi.buildcompatible.spi.AISyntheticBeanCreatorClassFactory`.

The internal build-compatible creators and extensions moved too
(`dev.langchain4j.cdi.buildcompatible.aiservice.*`, `...buildcompatible.plugin.*`), but those are discovered via
`META-INF/services` and are not referenced by name from consumer code.

## Known limitations

- **`langchain4j-cdi-a2a`** ships only an `Automatic-Module-Name` (`dev.langchain4j.cdi.a2a`). Its transitive
  dependencies `a2a-java-sdk-common` and `a2a-java-sdk-spec` are automatic modules that both export the package
  `org.a2aproject.sdk.util`; the module system forbids the same package in two modules, so an explicit
  `module-info.java` cannot be compiled until those upstream jars are modularized (or patched — the
  `jlink-vidocq` tooling can patch them by merging the split package).
- `langchain4j-cdi-build-compatible-ext` targets Quarkus/Helidon, which deploy on the class path. Do not place it
  together with `langchain4j-cdi-core` on a strict module path unless you also resolve any remaining differences —
  the traditional-server path uses `langchain4j-cdi-portable-ext`, which is split-free with core.

## Building a modular runtime image for vidocq (`jlink-vidocq` profile)

The optional, **opt-in** module `langchain4j-cdi-jlink` (activated by the `jlink-vidocq` profile, never deployed to
Maven Central) turns the module-path-friendly set into a fully-modular graph:

```bash
mvn -Pjlink-vidocq -pl langchain4j-cdi-jlink verify
```

It performs two steps:

### 1. License gate (redistribution safety)

Before patching/redistributing any third-party jar, the `license-maven-plugin` resolves the license of the **entire
dependency closure (transitive included)** and fails the build if any of them is **not** on the permissive
allow-list (Apache-2.0, MIT, BSD-2/3-Clause, EPL-1.0/2.0, EDL-1.0). This enforces the policy: *never patch and
redistribute a jar whose license does not allow it.* The resolved report is written to
`langchain4j-cdi-jlink/target/generated-sources/license/THIRD-PARTY.txt`.

Tune `<includedLicenses>` / `<licenseMerges>` in `langchain4j-cdi-jlink/pom.xml` against that report if a new
dependency reports its (permissive) license under a spelling that is not yet normalized. A dependency with a
non-permissive or missing license fails the build by design — resolve it (exclude the dependency, or obtain a
redistribution grant) rather than loosening the gate.

### 2. Patch non-modular third-party jars (ModiTect)

`moditect-maven-plugin` generates a `module-info` (requires/exports derived by `jdeps`) for the jars in the closure
that ship neither a `module-info` nor an `Automatic-Module-Name`, writing the modularized copies to
`langchain4j-cdi-jlink/target/modules/`. The seeded list covers the LangChain4j and MCP-Java jars and assigns the
same module names the first-party descriptors already `require`, so those names become stable/explicit:

`langchain4j`, `langchain4j.core`, `langchain4j.http.client`, `langchain4j.agentic`, `mcp.annotations`,
`mcp.model`, `mcp.server.api`.

### Extending to a full jlink image

`jlink` cannot link **automatic** modules — so a complete runtime image also requires patching every remaining
automatic-module jar of *your* closure (e.g. `jackson-*`, `slf4j-api`, `opennlp-tools`, `jspecify`). Enumerate the
closure and add each remaining jar to the ModiTect `<modules>` list the same way, then add a ModiTect
`create-runtime-image` execution listing your root modules. Enumerate the closure with:

```bash
jdeps --multi-release 17 --module-path "<assembled-module-path>" --list-deps <your-app-or-module>
```

Because these are multi-release jars, `--multi-release 17` is passed to `jdeps` (already configured for the ModiTect
step via `<jdepsExtraArgs>`).

## Testing note

The first-party jars ship a `module-info`, but their unit tests run on the **class path**
(`maven-surefire-plugin` `useModulePath=false`). This keeps `META-INF/services` test providers discoverable and
avoids module-system reflection restrictions in the Weld-based tests, while the published jars remain fully modular.
