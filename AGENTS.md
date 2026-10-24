# AGENTS.md

## Project

TypeJava is a small TypeScript-flavoured language that compiles to JVM bytecode with ASM. The
pipeline is lexer -> parser -> checker -> ASM code generator -> generated classloader.

Keep `README.md` user-facing. Put development, release, CI, and maintenance details here.

## Commands

Use the Nix dev shell:

```bash
nix develop --command gradle check
nix develop --command gradle build
```

`build` runs tests, produces the thin library jar, sources/javadoc jars, and the fat CLI jar.

Update lexer snapshots when token output intentionally changes:

```bash
nix develop --command env UPDATE_SNAPSHOTS=true \
  gradle test --tests 'cc.kilonode.typejava.lexer.LexerSnapshotTest' --rerun-tasks
```

Always run `nix develop --command gradle build` before finishing a change.

## Build outputs

- `build/libs/typejava-<version>.jar` — thin library jar for Maven consumers
- `build/libs/typejava-<version>-sources.jar`
- `build/libs/typejava-<version>-javadoc.jar`
- `build/libs/typejava-<version>-all.jar` — self-contained CLI jar

The CLI jar is built by the `cliJar` task and attached to `assemble`, so `gradle build` includes it.
Never make the normal `jar` fat: its POM declares ASM as a normal dependency.

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
git tag typejava-v0.1.0
git push origin typejava-v0.1.0
```

`.github/workflows/release.yml`:

1. Validates the tag against `typejava-vX.Y.Z`.
2. Requires the tagged commit to be on `main`.
3. Runs `gradle build publish -Pversion=<version>`.
4. Publishes `cc.kilonode:typejava:<version>` to GitHub Packages.
5. Creates a GitHub Release with the thin/sources/javadoc/CLI jars plus `SHA256SUMS`.

Prerelease versions create prerelease GitHub Releases. Do not move or reuse a published tag.

GitHub Packages requires authentication even for public Maven packages, using a classic PAT with
`read:packages`. Maven Central is the intended future path for credential-free consumption.

The Gradle publishing repository is only configured when `GITHUB_REPOSITORY` and `GITHUB_TOKEN` are
present and the version is not a `-SNAPSHOT`. This prevents accidental snapshot publishing.

## Architecture

- `lexer/`, `parser/`, `ast/` — front end
- `checker/` — nominal type checking, null narrowing, resolved symbols/types
- `codegen/` — ASM generation; object types get an abstract base class per nominal type and a
  subclass per method-bearing object-literal site
- `runtime/` — `Console`, `TjArray`, `TjMap`, and value formatting
- `TypeJava` — public `run` API and CLI entry point

## Constraints

- Java 21.
- Nominal object types; no structural assignability for named types.
- Only nullable unions: `T | null`.
- Generics: type declarations and explicit type arguments only.
- No imports/modules, exceptions, inheritance, overloads, lambdas/first-class functions,
  array/map literals, optional properties, `undefined`, general unions, type switches, or generic
  functions/methods.
- Compilation is fail-fast; no error recovery or warnings.
- Do not add source comments unless requested.
- Do not commit unless the user explicitly asks.
