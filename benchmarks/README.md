# Benchmarks

JMH benchmarks comparing the same log-classification / business-rule workload implemented in
plain Java, TypeJava, Groovy, BeanShell, and Rhino.

## Workload

Every implementation:

- builds 10,000 deterministic upstream log records;
- classifies each record by level and service-specific priority rules;
- applies an override that pages on `card declined`;
- aggregates severity and action counts in insertion-ordered maps;
- produces the same one-line summary.

The Java and TypeJava implementations use the same algorithm and closely matching data shapes.
The Groovy, BeanShell, and Rhino versions use each language's natural idioms (Groovy maps and
ranges, BeanShell raw Java-like collections, Rhino JavaScript objects and arrays), so cross-language
numbers are indicative rather than a strictly normalized code-generation comparison.

## Methodology

- Compilation/parsing is **not** measured. Every implementation is prepared once in `@Setup`,
  outside the measured region:
  - TypeJava uses the public `TypeJava.compile` API, instantiates the program once, and invokes
    its exported `runWorkload` function through a typed export bridge;
  - Groovy is parsed once into a `Script` and its `runWorkload` method is invoked;
  - BeanShell is evaluated once into an `Interpreter` namespace and its `runWorkload` method is
    invoked;
  - Rhino is compiled once into a `Script` and its `runWorkload` function is invoked;
  - Java has no preparation step.
- Every implementation runs 1,000 warmup executions in `@Setup` so the JVM JIT compiles hot paths
  before JMH starts. Override the count in forked JVMs with
  `-jvmArgsAppend -Dtj.benchmark.warmups=N`.
- JMH then applies its own warmup and measurement phases:
  `@Fork(1)`, 5 x 1 s warmup, 10 x 1 s measurement, average time in milliseconds.
- Every implementation returns the one-line summary directly to the harness. No console or
  `PrintStream` work is included in the measured region.
- Results are platform- and JVM-dependent. Compare runs on the same machine and JDK 21.

## Current results

Recorded on JDK 21, JMH 1.37, single fork and thread. One operation is one full 10,000-log run.

| Implementation | Time per operation | Throughput | Relative to Java |
|---|---:|---:|---:|
| Plain Java | 0.308 ± 0.002 ms | ~32.5M logs/s | 1.00x |
| TypeJava | 0.365 ± 0.002 ms | ~27.4M logs/s | 1.19x |
| Rhino (JavaScript) | 2.651 ± 0.028 ms | ~3.77M logs/s | 8.6x |
| Groovy | 3.302 ± 0.029 ms | ~3.03M logs/s | 10.7x |
| BeanShell | 128.297 ± 0.554 ms | ~78k logs/s | 417x |

Relative factors vary by a few percent between JVM forks.

## Run

Run all benchmarks:

```bash
nix develop --command gradle :benchmarks:jmh
```

Run all benchmarks with a shorter JMH smoke configuration:

```bash
nix develop --command gradle :benchmarks:jmh --args="LogClassificationBenchmark -f 1 -wi 2 -i 3 -w 1s -r 1s"
```

Run only TypeJava:

```bash
nix develop --command gradle :benchmarks:jmh --args="LogClassificationBenchmark.typeJava"
```

Build and test the workload without running JMH:

```bash
nix develop --command gradle :benchmarks:check
```

## Dependency isolation

Groovy, BeanShell, and Rhino are `implementation` dependencies of `:benchmarks` only. They must
never be added to `:lib`, which is the only published module. Verify with:

```bash
nix develop --command gradle :lib:dependencies --configuration runtimeClasspath
nix develop --command gradle :lib:generatePomFileForMavenPublication
```

## Layout

- `src/main/resources/benchmarks/log-classification.tj` — TypeJava workload
- `src/main/resources/benchmarks/log-classification.groovy` — Groovy workload
- `src/main/resources/benchmarks/log-classification.bsh` — BeanShell workload
- `src/main/resources/benchmarks/log-classification.js` — Rhino workload
- `src/main/java/.../JavaLogClassification.java` — plain Java workload
- `src/main/java/.../TypeJavaLogClassification.java` — TypeJava export bridge adapter
- `src/main/java/.../GroovyLogClassification.java` — Groovy adapter
- `src/main/java/.../BeanShellLogClassification.java` — BeanShell adapter
- `src/main/java/.../RhinoLogClassification.java` — Rhino adapter
- `src/main/java/.../LogClassificationBenchmark.java` — JMH entry point
- `src/test/java/.../LogClassificationWorkloadTest.java` — checks every implementation produces
  the same summary and that repeated compiled execution is stable
