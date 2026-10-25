
> ⚠️ **Work in progress.** TypeJava is usable, but the language and runtime are still changing.

# TypeJava

TypeScript-flavoured scripting on the JVM, compiled to bytecode with ASM.

TypeJava is a small, typed language for rule engines and embedded scripts. It reads like
TypeScript, compiles to bytecode, and does not try to be JavaScript or ship a browser runtime.

## Status

The example below compiles to JVM bytecode and runs. Compilation is fail-fast: the first lexical,
syntax, or type error aborts the run.

Named object types are nominal. The only union form is `T | null`; there is no `undefined` or
optional-property syntax. Generics are limited to type declarations and explicit type arguments.

Not supported yet: imports/modules, exceptions, inheritance, overloads, lambdas or first-class
function values, array/map literals, general unions, type switches, and generic functions/methods.

## Benchmarks

Same log-classification workload (10,000 logs, one operation = one full run), implemented
idiomatically in each language. Script parsing/compilation is excluded; each implementation gets
1,000 warmup runs before JMH measurement. JDK 21, JMH 1.37, single fork and thread.

| Implementation | Time per operation | Throughput | Relative to Java |
|---|---:|---:|---:|
| Plain Java | 0.308 ± 0.002 ms | ~32.5M logs/s | 1.00x |
| TypeJava | 0.365 ± 0.002 ms | ~27.4M logs/s | 1.19x |
| Rhino (JavaScript) | 2.651 ± 0.028 ms | ~3.77M logs/s | 8.6x |
| Groovy | 3.302 ± 0.029 ms | ~3.03M logs/s | 10.7x |
| BeanShell | 128.297 ± 0.554 ms | ~78k logs/s | 417x |

Times are averages with 99.9% confidence intervals. Results are machine- and JVM-dependent, and
relative factors vary by a few percent between runs.

Reproduce with:

```bash
nix develop --command gradle :benchmarks:jmh
```

The Groovy, BeanShell, and Rhino runtimes are benchmark-only dependencies and are not part of the
published library.

## Install

Java 21 is required. Stable versions are published from `typejava-vX.Y.Z` tags; use the latest
version from [GitHub Releases](https://github.com/dawidcxx/typejava/releases).

### Gradle

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/dawidcxx/typejava")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("cc.kilonode:typejava:0.4.0")
}
```

### Maven

Add the GitHub Packages repository:

```xml
<repository>
  <id>github</id>
  <url>https://maven.pkg.github.com/dawidcxx/typejava</url>
</repository>
```

Then depend on it:

```xml
<dependency>
  <groupId>cc.kilonode</groupId>
  <artifactId>typejava</artifactId>
  <version>0.4.0</version>
</dependency>
```

Add a `github` server to `~/.m2/settings.xml` with your GitHub username and a classic personal
access token with `read:packages`. GitHub Packages requires authentication even for public
packages.

### CLI

Download `typejava-<version>-all.jar` from Releases:

```bash
java -jar typejava-0.4.0-all.jar script.tj
```

### Java API

```java
Environment environment = TypeJava.environment()
    .io(new Io() {
      @Override
      public void println(String line) {
        System.err.println("[tj] " + line);
      }
    })
    .withStd()
    .build();

TypeJava.run(source, environment);

CompiledProgram program = TypeJava.compile(source, environment);
program.run();
program.run();
```

`Io` controls console output; when no implementation is supplied the environment uses
`DefaultIo`, which writes to `System.out`. `withStd()` binds `TjArray` and `TjMap`; without it those
types are unavailable. `TypeJava.compile` parses, checks, and generates bytecode once.
`CompiledProgram` is immutable and reusable; `program.run()` executes top-level initialization in a
fresh instance. Runtime exceptions from the generated program are allowed to escape unchanged.

### Exported functions

Java records can be bound as readonly TypeJava types, and top-level `export function` declarations
can be invoked from Java:

```java
public record Payload(double incrementMe) {}

public interface LogApi {
  Payload analyzeLog(Payload payload);
}

Environment environment = TypeJava.environment()
    .record(Payload.class)
    .exports(LogApi.class)
    .build();

String source = """
    let invocations: number = 0;

    export function analyzeLog(payload: Payload): Payload {
      invocations = invocations + 1;
      return { incrementMe: payload.incrementMe + 1 };
    };
    """;

CompiledProgram program = TypeJava.compile(source, environment);
ProgramInstance instance = program.instantiate();
LogApi api = instance.exports(LogApi.class);
Payload result = api.analyzeLog(new Payload(0));
```

Each `instantiate()` call owns isolated top-level state. A `ProgramInstance` is not thread-safe;
create one per thread. Reference record components are non-null by default; annotate a component
with `@MaybeNull` to map it to `T | null`.

## Example

```ts
type UpstreamLog = {
  readonly service: string;
  readonly message: string;
  readonly level: number | null;
};

type LogClassification = {
  service: string;
  severity: string;
  action: string;
};

type Summary = {
  total: number;
  critical: number;
};

type Box<T> = {
  value: T;
};

type Classifier = {
  classify: (this: Classifier, log: UpstreamLog) => LogClassification;
  explain: (this: Classifier, classification: LogClassification) => string;
};

function severityOf(level: number | null): string {
  if (level == null) {
    return "unknown";
  }

  if (level >= 5) {
    return "critical";
  }

  if (level >= 3) {
    return "warning";
  }

  return "info";
};

const classifier: Classifier = {
  classify(this: Classifier, log: UpstreamLog): LogClassification {
    let action: string = "watch";
    const severity: string = severityOf(log.level);

    if (severity == "critical") {
      action = "page";
    } else if (severity == "warning") {
      action = "ticket";
    }

    return {
      service: log.service,
      severity: severity,
      action: action
    };
  },

  explain(this: Classifier, classification: LogClassification): string {
    return classification.service + "/" + classification.severity
      + " -> " + classification.action;
  }
};

function firstCritical(logs: TjArray<UpstreamLog>): UpstreamLog | null {
  let i: number = 0;

  while (i < logs.size()) {
    const log: UpstreamLog = logs.get(i);

    if (log.level == null) {
      i = i + 1;
      continue;
    }

    if (log.level >= 5) {
      return log;
    }

    i = i + 1;
  }

  return null;
};

const logs: TjArray<UpstreamLog> = TjArray.create();

logs.add({
  service: "billing",
  message: "card declined",
  level: 5
});

logs.add({
  service: "auth",
  message: "slow login",
  level: 3
});

logs.add({
  service: "cron",
  message: "nothing to report",
  level: null
});

const buckets: TjMap<string, TjArray<LogClassification>> = TjMap.create();
const summary: Summary = { total: 0, critical: 0 };

let i: number = 0;
while (i < logs.size()) {
  const log: UpstreamLog = logs.get(i);
  const result: LogClassification = classifier.classify(log);

  summary.total = summary.total + 1;

  if (result.severity == "critical") {
    summary.critical = summary.critical + 1;
  }

  if (buckets.containsKey(result.severity)) {
    const bucket: TjArray<LogClassification> = buckets.get(result.severity);
    bucket.add(result);
  } else {
    const bucket: TjArray<LogClassification> = TjArray.create();
    bucket.add(result);
    buckets.put(result.severity, bucket);
  }

  console.log(classifier.explain(result));
  i = i + 1;
}

const worst: UpstreamLog | null = firstCritical(logs);
if (worst != null) {
  console.log("first critical: " + worst.service);
}

const boxed: Box<UpstreamLog> = { value: logs.get(0) };
console.log("boxed service: " + boxed.value.service);
console.log("classified " + summary.total + ", critical " + summary.critical);
```
