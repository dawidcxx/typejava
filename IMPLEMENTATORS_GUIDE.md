# Implementor Guide

This guide is for Java developers embedding TypeJava in an application. It covers the public compile
and run API, isolated program instances, exported functions, Java record bindings, custom native
types, TypeScript declaration generation, and the language semantics that matter at the Java
boundary.

The language itself is covered in the [TypeJava guide](TJ_GUIDE.md).

## Requirements and dependency

TypeJava requires Java 21. The published coordinates are:

```text
cc.kilonode:typejava:<version>
```

See [Install](README.md#install) in the README for Gradle, GitHub Packages authentication, and the
CLI jar. This guide starts with the Java API.

## Compile and run

The simplest entry point is:

```java
TypeJava.run("console.log(\"hello\");", Environment.standard());
```

`Environment.standard()` binds `TjArray` and `TjMap` and writes `console.log` output to
`System.out`. Build a custom environment when you need different output, records, export contracts,
or native types.

### Custom environment and output

`Io` receives already formatted console lines. It has a default `println` method, so implementations
are usually anonymous classes:

```java
List<String> lines = new ArrayList<>();

Environment environment =
    TypeJava.environment()
        .io(
            new Io() {
              @Override
              public void println(String line) {
                lines.add(line);
              }
            })
        .withStd()
        .build();

TypeJava.run("console.log(\"hello\", 42);", environment);
```

`withStd()` is shorthand for `withArray().withMap()`. Without it, `TjArray` and `TjMap` are unknown
types. `withArray()` and `withMap()` can be used separately.

### Compile once, instantiate many times

```java
CompiledProgram program = TypeJava.compile(source, environment);

program.run();  // creates and runs one instance
program.run();  // creates a separate instance with fresh top-level state
```

`CompiledProgram` is immutable and can be shared between threads. `run()` is a convenience that
instantiates a program and discards the instance.

Use `instantiate()` when you need to call into the program:

```java
ProgramInstance first = program.instantiate();
ProgramInstance second = program.instantiate();
```

Each instance has its own copy of top-level variables. A `ProgramInstance` is not thread-safe;
create one instance per thread, or synchronize access. Instances are reclamation-managed; there is
no explicit close operation.

`runFile(Path, Environment)` is a convenience for reading a source file and running it. It throws
`IOException` if the file cannot be read.

### Public API at a glance

| API | Purpose |
|---|---|
| `TypeJava.run(source, environment)` | Compile and run one instance. |
| `TypeJava.compile(source, environment)` | Parse, check, and generate bytecode once. |
| `TypeJava.runFile(path, environment)` | Read, compile, and run a `.tj` file. |
| `CompiledProgram.run()` | Instantiate and run, discarding the instance. |
| `CompiledProgram.instantiate()` | Create an isolated `ProgramInstance`. |
| `CompiledProgram.exports()` | Inspect exported function signatures. |
| `ProgramInstance.invoke(name, args...)` | Call an export without a typed interface. |
| `ProgramInstance.exports(Api.class)` | Get a typed proxy over exports. |
| `Environment.typeScriptDeclarations()` | Generate ambient `.d.ts` contents. |

## Exported functions

Exported functions are top-level functions marked with `export`. They are the supported bridge from
Java into a program instance.

### Java records as TypeJava types

Bind a Java record with `record(Class<?>)`:

```java
public record Payload(double incrementMe) {}

public interface LogApi {
  Payload analyzeLog(Payload payload);

  double invocationCount();
}
```

```java
Environment environment =
    TypeJava.environment()
        .record(Payload.class)
        .exports(LogApi.class)
        .build();
```

The record's simple name becomes the TypeJava type name. TypeJava source can use `Payload` directly,
without declaring it:

```ts
let invocations: number = 0;

export function analyzeLog(payload: Payload): Payload {
  invocations = invocations + 1;
  return { incrementMe: payload.incrementMe + 1 };
};

export function invocationCount(): number {
  return invocations;
};
```

The source may also redeclare the record as an identical readonly object type. A different
redeclaration is a compile error; the bound Java record remains authoritative.

### Call exports

```java
CompiledProgram program = TypeJava.compile(source, environment);
ProgramInstance instance = program.instantiate();

LogApi api = instance.exports(LogApi.class);
Payload result = api.analyzeLog(new Payload(0));

Object dynamic = instance.invoke("analyzeLog", new Payload(1));
```

`exports(LogApi.class)` returns a JDK proxy. Every abstract method on the interface must have a
matching exported function with a compatible name, arity, and host signature. The proxy call is
not cached; each method call goes through the same invocation path as `invoke`.

`program.exports()` returns a map of exported names to `ExportSignature` values. This is useful for
diagnostics or building dynamic adapters.

### Export contracts

`exports(Class<?>...)` records expected interfaces. During compilation, every abstract method in the
interface must have a matching `export function`; otherwise compilation fails. Extra exported
functions are allowed.

Exported functions must use types that can cross the host boundary:

- `number`, `string`, `boolean`, and `void`.
- Nullable variants using the Java wrapper for primitives and `String` for strings.
- Bound record classes.
- Bound native types such as `TjArray` and `TjMap`.

The host signature mapping is checked at compile time. A mismatch between the TypeJava type and the
Java interface method is a compile error, not a runtime surprise.

### Record rules

- The record and all enclosing classes must be public.
- The record must not declare type parameters.
- Record component names become TypeJava field names.
- Bound records must have unique simple names; the simple name is the TypeJava type name.
- A record nested inside another record must also be bound.
- Record components can use `double`, `int`, `boolean`, `String`, `Double`, `Boolean`, or another
  bound record.
- Non-null reference components are validated recursively when a record crosses an export
  parameter boundary. Nullable components are allowed to be null.
- Record-backed TypeJava fields are readonly.

Annotate a record component with `@MaybeNull` to map a reference component to `T | null`:

```java
public record Labeled(double value, @MaybeNull String label) {}
```

Primitive components cannot use `@MaybeNull`. For record components, `Double` and `Boolean` are
treated as non-null unless annotated; the annotation is what makes them nullable.

Source redeclaration of a bound record, when used, must match the record order-independently: same
field set, same types, and `readonly` on every field.

### Boundary validation

At an exported function entry point:

- Non-null `String` parameters are checked.
- Non-null record parameters are checked recursively.
- Nullable records and strings are allowed to be null.
- Non-null native parameters, including `TjArray` and `TjMap`, are checked for the outer reference,
  but their contents are not inspected.
- Exported return values are not deeply validated by generated code.

The nested checks exist because Java records can contain nulls that TypeJava's type system says are
impossible. A failure is an `IllegalArgumentException` that includes the parameter or component
path. Treat a boundary failure as a host integration error.

## Custom native types

A native type exposes an existing Java class to TypeJava. Native types are how `TjArray`, `TjMap`,
and your own standard-library-like APIs are bound.

```java
public final class Counter {
  private double value;

  public static Counter create() {
    return new Counter();
  }

  public void add(double amount) {
    value += amount;
  }

  public double value() {
    return value;
  }
}
```

```java
NativeType counter =
    NativeType.of("Counter", Counter.class)
        .factory("create", "()Lcom/example/Counter;", "Counter")
        .instanceMethod("add", "(D)V", "void", "number")
        .instanceMethod("value", "()D", "number");

Environment environment = TypeJava.environment().bind(counter).build();
```

```ts
const counter: Counter = Counter.create();
counter.add(2);
counter.add(3);
console.log(counter.value());
```

### Native type definitions

`NativeType.of(name, hostClass)` creates a definition. The name is the TypeJava name and must be a
valid identifier.

- `typeParameters("T", ...)` declares generic parameters.
- `factory(name, descriptor, returnType)` declares a zero-argument factory callable as
  `Type.create()`.
- `staticMethod(name, descriptor, returnType, parameterTypes...)` declares a static method.
- `instanceMethod(name, descriptor, returnType, parameterTypes...)` declares a method callable on
  values of that type.

The descriptor is the JVM method descriptor, for example `(ILjava/lang/String;)Z`. The return type
and parameter types after the descriptor are TypeJava type references such as `number`, `string`,
`T`, `TjArray<T>`, or another native type name.

Factories are special: TypeJava requires the destination to be annotated, because
`Type.create()` has no arguments from which to infer type parameters:

```ts
const numbers: TjArray<number> = TjArray.create();
```

Native methods are direct calls. Method references are not values and cannot be stored or passed
around. Bind a native type before compiling; definitions are immutable once resolved.

If a JVM method starts with an `Environment` parameter, the runtime injects the active environment
automatically. Such a descriptor starts with `(Lcc/kilonode/typejava/Environment;`. Do not include
the environment in the TypeJava parameter type list:

```java
NativeType.of("Host", Host.class)
    .staticMethod(
        "print",
        "(Lcc/kilonode/typejava/Environment;Ljava/lang/String;)V",
        "void",
        "string");
```

The built-in `TjArray` and `TjMap` definitions in `Environment` are complete examples of factories,
instance methods, generic parameters, and type variables.

## TypeScript declarations

`Environment.typeScriptDeclarations()` generates an ambient `.d.ts` file from the environment:

```java
Files.writeString(Path.of("typejava.d.ts"), environment.typeScriptDeclarations());
```

The generated file contains:

- Native types as interfaces, plus a companion `declare const` for static methods and factories.
- Bound records as readonly interfaces.
- Export interfaces from `exports(...)`.

Generic native types keep their type parameters. For example, the standard array produces:

```ts
interface TjArray<T> {
  size(): number;
  get(arg0: number): T;
  add(arg0: T): void;
}

declare const TjArray: {
  create<T>(): TjArray<T>;
};
```

The declarations describe the environment, not type aliases declared only inside a script. Bind a
record or native type if TypeScript tooling needs to know about it.

### Nullability in declarations

Java erasure and `String` cannot express whether a reference was nullable. For export interfaces:

- `@MaybeNull` on a parameter or method return adds `| null`.
- `Double` and `Boolean` are emitted as nullable numbers and booleans.
- `@MaybeNull` on interface methods is a declaration hint. It does not change runtime validation.

Generic arguments on export interface methods are erased, so they appear as `unknown`:

```ts
interface ArrayApi {
  numbers(): TjArray<unknown>;
  size(arg0: TjArray<unknown>): number;
}
```

### Monaco and other TypeScript tooling

The output is an ambient declaration file, so it can be included directly in a TypeScript project or
loaded into Monaco:

```ts
fetch("/typejava.d.ts")
    .then((response) => response.text())
    .then((declarations) => {
      monaco.languages.typescript.typescriptDefaults.addExtraLib(
          declarations,
          "file:///typejava.d.ts"
      );
    });
```

The declarations give TypeScript-powered completion and diagnostics access to TypeJava types. They
do not make Monaco parse `.tj` files by themselves; a TypeJava language contribution still needs
tokens, bracket rules, and a way to route the file to a language service.

For a `tsconfig`, include the file normally:

```json
{
  "include": ["src/**/*.ts", "typejava.d.ts"]
}
```

## Semantics at the boundary

### Nominal types

Object types are nominal. A Java record becomes one named TypeJava type. Two records or object types
with identical fields are not interchangeable. When crossing the boundary, use the bound record
class or native type the script expects; do not rely on structural shape.

### Immutability

- `readonly` is a compile-time TypeJava check and is shallow.
- `const` prevents rebinding a variable, not mutating an object.
- `TjArray` and `TjMap` are mutable.
- Java records are immutable in practice because their components are final.
- Returned or passed arrays and maps are the same mutable runtime objects, not copies.

If scripts must not mutate host collections, expose a read-only native wrapper instead of giving
them a mutable type.

### Numbers

Every TypeJava `number` is a JVM `double`.

- Export interface methods use `double` for non-null numbers and `Double` for nullable numbers.
- Record components may use `double` or `int`; both appear as TypeJava `number`.
- There is no integer, long, float, or big-decimal distinction in the language.
- Whole-number doubles are formatted without a decimal suffix by `console.log`.

### Top-level state

Top-level variables are instance fields. `CompiledProgram.instantiate()` creates fresh state.
There are no process-wide TypeJava globals. Functions and object methods can read and mutate
top-level state within their instance.

### Equality

- Numbers and booleans compare by value.
- Strings compare by value.
- Nullable values can be compared with `null`.
- Java records compare using the record's generated `equals`.
- Ordinary TypeJava object literals have no generated `equals` override and compare by identity.

### Closures and function values

There are no closures or first-class functions. A top-level function cannot capture local variables.
Object methods are the only way to attach behavior to data, and methods cannot be extracted as
values. Design host APIs around direct function and method calls.

### Exceptions

TypeJava has no language-level exceptions, `throw`, or `try` / `catch`.

- Compile failures throw `LexerException`, `ParseException`, or `CheckException`.
- Runtime exceptions from generated code and native methods escape to the caller.
- Checked exceptions thrown by native methods are wrapped in `RuntimeException` by the invocation
  bridge.

### Fail-fast compilation

Compilation stops at the first lexical, syntax, or type error. There is no error recovery and there
are no warnings. Compile before serving traffic, and treat a compile failure as a configuration
error for the script or rule set.

## Maintenance

Build, test, release, CI, and benchmark commands are in [AGENTS.md](AGENTS.md).
