# AGENTS.md

## Project

TypeJava is a small TypeScript-flavoured language that compiles to JVM bytecode with ASM. The
pipeline is lexer -> parser -> checker -> ASM code generator -> generated classloader.

Keep `README.md` user-facing. Put development, release, CI, and maintenance details here.

## Modules

- `:lib` — compiler, runtime, and public `TypeJava` compile/run API. This is the only published
  module.
- `:cli` — command-line entry point, thin CLI jar, and self-contained fat jar. Depends on `:lib`.
- `:examples` — runnable public-API demos. Depends only on `:lib`.
- `:benchmarks` — JMH harness comparing TypeJava with plain Java, Groovy, BeanShell, and Rhino.
  Depends on `:lib` plus benchmark-only language runtimes.

Dependency direction is `:cli -> :lib`, `:examples -> :lib`, and `:benchmarks -> :lib`. `:lib` must
not depend on any of them. Do not publish `:cli`, `:examples`, or `:benchmarks` yet. Groovy,
BeanShell, and Rhino must stay `implementation` dependencies of `:benchmarks` and must never appear
in `:lib` or its published POM.

## Commands

Use the Nix dev shell:

```bash
nix develop --command gradle check
nix develop --command gradle build
```

`build` runs all module tests, produces the thin library jar, sources/javadoc jars, and the fat CLI
jar.

Run the CLI:

```bash
nix develop --command gradle :cli:run --args="path/to/script.tj"
```

Run all examples:

```bash
nix develop --command gradle :examples:run
```

Run one example:

```bash
nix develop --command gradle :examples:run --args="objects"
```

Run the JMH benchmarks (compilation is excluded; see `benchmarks/README.md`):

```bash
nix develop --command gradle :benchmarks:jmh
```

Run a single benchmark:

```bash
nix develop --command gradle :benchmarks:jmh --args="LogClassificationBenchmark.typeJava"
```

Update lexer snapshots when token output intentionally changes:

```bash
nix develop --command env UPDATE_SNAPSHOTS=true \
  gradle :lib:test --tests 'cc.kilonode.typejava.lexer.LexerSnapshotTest' --rerun-tasks
```

Always run `nix develop --command gradle build` before finishing a change.

## Build outputs

- `lib/build/libs/typejava-<version>.jar` — thin library jar for Maven consumers
- `lib/build/libs/typejava-<version>-sources.jar`
- `lib/build/libs/typejava-<version>-javadoc.jar`
- `cli/build/libs/typejava-<version>-all.jar` — self-contained CLI jar
- `examples/build/libs/typejava-examples-<version>.jar` — examples module

The CLI fat jar is built by the `:cli:cliJar` task and attached to `:cli:assemble`, so root
`gradle build` includes it. Never make the `:lib` jar fat: its POM declares ASM as a normal
dependency.

The published Maven coordinates stay `cc.kilonode:typejava:<version>` even though the module
directory is `lib`. Do not publish under `cc.kilonode:lib`.

Version resolution:

- Release CI passes `-Pversion=<tag-version>`.
- Local builds fall back to `VERSION_NAME` in `gradle.properties`.
- Never mutate `gradle.properties` in CI to set a release version.

## CI

`.github/workflows/ci.yml` runs on pull requests and pushes to `main` only. It runs
`gradle --no-daemon build`, caches Gradle state, and cancels superseded runs. There are no
snapshot-building or snapshot-publishing workflows.

## Releases

Only tagged commits are published. Internal release rule: **no snapshots, no per-commit packages,
no untagged releases.**

Tag format: `typejava-vX.Y.Z` (full SemVer; prerelease suffixes are allowed, e.g.
`typejava-v0.2.0-rc.1`).

```bash
git tag typejava-v0.2.0
git push origin typejava-v0.2.0
```

`.github/workflows/release.yml`:

1. Validates the tag against `typejava-vX.Y.Z`.
2. Requires the tagged commit to be on `main`.
3. Runs `gradle build :lib:publish -Pversion=<version>`.
4. Publishes only `cc.kilonode:typejava:<version>` to GitHub Packages.
5. Creates a GitHub Release with the thin/sources/javadoc/CLI jars plus `SHA256SUMS`.

Prerelease versions create prerelease GitHub Releases. Do not move or reuse a published tag.
Never run bare root `publish`; publish `:lib:publish` explicitly.

GitHub Packages requires authentication even for public Maven packages, using a classic PAT with
`read:packages`. Maven Central is the intended future path for credential-free consumption.

The Gradle publishing repository is only configured when `GITHUB_REPOSITORY` and `GITHUB_TOKEN` are
present and the version is not a `-SNAPSHOT`. This prevents accidental snapshot publishing.

## Architecture

- `:lib` `lexer/`, `parser/`, `ast/` — front end
- `:lib` `checker/` — nominal type checking, null narrowing, resolved symbols/types, record
  bindings, and export signature validation
- `:lib` `codegen/` — ASM generation; object types get an abstract base class per nominal type and
  a subclass per method-bearing object-literal site; record-bound types use the host record class;
  generated programs are instance-based with isolated top-level state
- `:lib` `runtime/` — `Console`, `TjArray`, `TjMap`, `Boundary`, and value formatting
- `:lib` `TypeJava` / `CompiledProgram` / `ProgramInstance` — public `compile`/`run`/`runFile`,
  `instantiate`, and exported-function bridge API plus the generated classloader
- `:lib` `Environment` — `Io`, native types, record bindings, and expected export interfaces
- `:cli` `Main` — argument parsing, file execution, usage, and exit codes
- `:examples` `Main` — loads `.tj` resources and demonstrates only the public `TypeJava` API
- `:benchmarks` — precompiles/parses every language workload in JMH setup so script compilation is
  never measured; compares Java, TypeJava, Groovy, BeanShell, and Rhino on the same business rules

## Constraints

- Java 21.
- Nominal object types; no structural assignability for named types.
- Only nullable unions: `T | null`.
- Generics: type declarations and explicit type arguments only.
- No imports/modules, exceptions, inheritance, overloads, lambdas/first-class functions,
  array/map literals, optional properties, `undefined`, general unions, type switches, or generic
  functions/methods.
- Compilation is fail-fast; no error recovery or warnings.
- Examples must not depend on test fixtures or compiler internals.
- Do not add source comments unless requested.
- Do not commit unless the user explicitly asks.
