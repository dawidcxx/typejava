
> ⚠️ **Work in progress.** The language, standard library, and this README are subject to change. Nothing really works yet and may never be finished.

> ⚠️ **AI slop.** But still probably better than other solutions out there

# TypeJava

TypeScript-flavoured scripting on the JVM, compiled to bytecode with ASM.

It is not TypeScript, and it is not trying to be JavaScript. TypeJava is a small, typed language for
rule engines and embedded scripts. It reads like TypeScript, compiles to bytecode, and has no
interest in shipping a browser runtime.

## Status

Early development. The lexer and parser cover the example below, including generic type
declarations and applications; the checker, code generator, and runtime are still being built.
`TjArray`, `TjMap`, and the compatibility reference are WIP parts of the future standard library.

Generics are deliberately small: type parameters on type declarations and explicit type arguments
on named types. There are no user-declared generic functions or methods yet.

`T | null` is the absence representation. There is no `undefined`, no optional-property syntax, and
no general unions: the only union form is a nullable pair with `null`. Normalize incoming optionals
and nullables at the boundary.

## Example

```ts
/// <reference path="typed-java-compatibility" />

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

## Requirements

- Java 21
- Nix with flakes (recommended; provides Gradle and the toolchain)

The Gradle wrapper is intentionally not checked in. Gradle comes from the Nix dev shell.

## Development

```bash
direnv allow        # or: nix develop
gradle check
gradle build
```

Without entering the shell:

```bash
nix develop --command gradle check
```

Build outputs land in `build/libs/`.

## Artifacts

- Group: `cc.kilonode`
- Artifact: `typejava`
- Version: see `VERSION_NAME` in `gradle.properties`
- Java: 21
