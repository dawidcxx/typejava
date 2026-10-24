# TypeJava

TypeScript-like scripting on the JVM, compiled to bytecode with ASM.

TypeJava is a small scripting language with syntax that will feel familiar to TypeScript users. It is
**not** TypeScript and does not emulate JavaScript: there are no prototypes, no implicit `this`
juggling, and no full ECMAScript object model. Types are checked, code is compiled ahead of time to
JVM bytecode, and the generated code is meant to be fast.

The primary target is rule engines and other embedded scripting use cases where predictable
performance matters more than matching every JavaScript quirk.

## Design goals

- **Familiar syntax.** TypeScript-style declarations, expressions, and control flow.
- **Not TypeScript.** No prototype chain, no dynamic `this`, no browser/Node APIs.
- **Built for rules and scripts.** Small surface area, fast startup, embeddable in Java apps.
- **Real bytecode.** Source is compiled with the Java ASM library, not interpreted.
- **Performance over parity.** Some JavaScript semantics are deliberately skipped to avoid boxing
  and keep generated code efficient.

## Status

Early development. The language, compiler, and runtime are not implemented yet; the current public
API is a placeholder.

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
