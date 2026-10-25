# The TypeJava Guide

This guide teaches the TypeJava language. It assumes you have written some TypeScript or another
C-like typed language, so it focuses on what TypeJava supports and where it deliberately differs.

TypeJava is not JavaScript and is not a TypeScript implementation. It compiles a single source file
to JVM bytecode. There is no browser runtime, module loader, `undefined`, or prototype chain.

## A complete program

Here is a small but complete TypeJava program. It declares nominal object types, uses a nullable
number and `TjArray`, implements an object method with `this`, mutates program state in a `while`
loop, and builds a result:

```ts
type Item = {
  readonly name: string;
  readonly quantity: number | null;
};

type Report = {
  total: number;
  missing: TjArray<string>;
};

type Builder = {
  build: (this: Builder, items: TjArray<Item>) => Report;
};

const builder: Builder = {
  build(this: Builder, items: TjArray<Item>): Report {
    const report: Report = {
      total: 0,
      missing: TjArray.create()
    };

    let i: number = 0;
    while (i < items.size()) {
      const item: Item = items.get(i);

      if (item.quantity == null) {
        report.missing.add(item.name);
      } else {
        report.total = report.total + item.quantity;
      }

      i = i + 1;
    }

    return report;
  }
};

const items: TjArray<Item> = TjArray.create();
items.add({ name: "bolts", quantity: 12 });
items.add({ name: "nuts", quantity: null });

const report: Report = builder.build(items);
console.log("total:", report.total);
console.log("missing:", report.missing.size());
```

The rest of this guide explains the pieces used above, then calls out the semantic differences from
TypeScript.

## A program is a single file

A TypeJava program contains top-level declarations and statements:

```ts
type Message = {
  readonly text: string;
};

const message: Message = { text: "hello" };

function render(value: Message): string {
  return "[" + value.text + "]";
};

console.log(render(message));
```

All type declarations, functions, variables, and executable statements live in one file. There are
no imports or language modules; a program has a single compilation unit. The `export` keyword
exists only to expose a function to the Java host; see the
[implementor guide](IMPLEMENTATORS_GUIDE.md).

Executable top-level statements run when the program is instantiated or run. Compilation is
fail-fast: the first lexical, syntax, or type error stops compilation.

## Lexical basics

- Comments use `//` for one line and `/* ... */` for blocks.
- Strings use double quotes only. Single quotes and backticks are errors. The usual escapes such as
  `\"`, `\\`, `\n`, `\t`, and `\u0041` are supported.
- Numbers are written as digits with an optional decimal part, for example `42` or `12.5`. They are
  always represented as JVM `double`; there is no separate integer type. Hex literals, exponents,
  and numeric separators are not supported.
- `true`, `false`, and `null` are literals.
- Statements and declarations end with semicolons. This is not optional. Control-flow blocks do not
  take a semicolon after the closing brace, but function declarations and type aliases do.

## Types

The primitive types are `number`, `string`, `boolean`, and `void`. `void` is a return type, not a
storable value type. `null` is a literal and a type only in the nullable form described below.

Every variable, parameter, and return type is written explicitly. Type annotations and type
arguments are never inferred except for native factory calls such as `TjArray.create()`, which
require a contextual type.

### Type aliases

`type` gives a name to another type:

```ts
type UserId = string;
type MaybeName = string | null;
type Scores = TjMap<string, number>;
```

Aliases can be generic:

```ts
type Box<T> = {
  value: T;
};

type Registry<T> = TjMap<string, TjArray<T>>;
```

Generic parameters are allowed on type declarations only. Functions and methods cannot declare
their own type parameters. Type arguments must be written explicitly at each use; there is no
generic function inference.

### Object types are nominal

An object type declares fields and, optionally, function-typed fields:

```ts
type User = {
  name: string;
  score: number | null;
};

type Scoreboard = {
  summarize: (this: Scoreboard, user: User) => string;
};
```

Object types are nominal. Two separately declared types with identical fields are not
interchangeable, unlike TypeScript's structural typing:

```ts
type Left = { value: number; };
type Right = { value: number; };

const left: Left = { value: 1 };
const right: Right = left; // error: Left is not Right
```

An object literal must have a contextual nominal object type, usually from a variable annotation,
parameter, field, or return position. Every field must be provided, and unknown fields are errors.

An alias that resolves to an object type creates a nominal type. Aliases that resolve to primitives,
collections, nullable types, or other non-object types are transparent aliases.

Inline object types are allowed where a type is expected:

```ts
function xOf(point: { x: number; }): number {
  return point.x;
};

console.log(xOf({ x: 4 }));
```

The inline shape is still resolved as a nominal type internally.

### Readonly fields

Fields can be marked `readonly`:

```ts
type Point = {
  readonly id: string;
  x: number;
  y: number;
};
```

`readonly` is checked at compile time. It prevents assigning to that field, but it does not freeze
the object and is not deep. A `readonly` field can still reference a mutable object. As with
TypeScript, `readonly` is shallow.

### Nullability

The only union form is nullable: `T | null`. There is no `undefined`, optional-property syntax,
general union, or intersection type.

```ts
type Log = {
  readonly level: number | null;
  readonly message: string;
};
```

You must check for null before using a nullable value:

```ts
function describe(level: number | null): string {
  if (level == null) {
    return "unknown";
  }

  return "level " + level;
};
```

The checker narrows `T | null` to `T` inside `if (value != null)` and after an early
`if (value == null) { return ...; }`. The same narrowing works for fields. Narrowing is forgotten
after an assignment, and field narrowing is forgotten after calls that could mutate the object.

Use `== null` and `!= null`. There is no optional chaining (`?.`), nullish coalescing (`??`), or
non-null assertion (`!`) operator. The `!` operator is boolean negation only.

### Function types

Function types may be used only as the type of an object field, directly or through an alias. They
use arrow syntax and may declare a `this` parameter first:

```ts
type Counter = {
  value: number;
  increment: (this: Counter) => number;
};

type Adder = {
  add: (left: number, right: number) => number;
};
```

Parameter names in a function type are documentation; implementations choose their own names. If
the function type declares `this: SomeType`, the implementation must declare a matching `this`
parameter first.

Functions are not first-class values. You cannot store a function in a variable, pass it as an
argument, or return it. Function types describe object methods that are called directly.

## Variables and scope

Declare variables with `let` or `const`. Both require an explicit type and an initializer:

```ts
let total: number = 0;
const label: string = "total";

total = total + 1;
label = "other"; // error: cannot assign to constant
```

`const` prevents reassigning the variable, not mutating an object it points to. `let` allows
reassignment. Parameters can be reassigned.

Declarations are block-scoped. Local variables and parameters cannot use the built-in names
`console` or a bound native type name. A variable cannot be redeclared in the same scope.

Top-level variables are program state. Each instantiated program gets its own copy, and that state
lives until the instance is discarded. Top-level initializers run in source order, so do not read a
top-level variable before its initializer has run.

## Functions

Top-level functions are declared with `function`:

```ts
function add(left: number, right: number): number {
  return left + right;
};

console.log(add(2, 3));
```

- Parameters and return types are mandatory.
- A non-void function must return a value on every path.
- A `void` function can use a bare `return;` or fall off the end.
- Functions may be recursive.
- Functions cannot be nested inside other functions.
- There are no overloads, default parameters, optional parameters, rest parameters, or generic
  function parameters.
- There are no closures or first-class function values.

Functions can read and mutate top-level state. They cannot capture local variables because nested
functions do not exist.

Exported functions are declared with `export function` and are covered in the
[implementor guide](IMPLEMENTATORS_GUIDE.md).

## Object literals and methods

Object literals use `field: value` pairs and method shorthand for function-typed fields:

```ts
type Person = {
  name: string;
  age: number;
  greet: (this: Person) => string;
};

const alice: Person = {
  name: "Alice",
  age: 30,
  greet(this: Person): string {
    return "Hi, I'm " + this.name;
  }
};
```

Method rules:

- A method for a function-typed field must use method syntax, not `field: (x) => { ... }`.
- If the field type declares `this: Person`, the method must declare `this: Person` as its first
  parameter.
- If the field type does not declare `this`, the method must not declare one.
- `this` is available only inside object methods. Calling `obj.method(...)` supplies the receiver
  automatically.
- Readonly fields cannot be assigned. Mutable fields can be assigned by methods.

Object literals are not structural. Their expected type comes from context, and the literal must
match the named or inline object type exactly.

## Control flow

`if`, `else`, and `else if` work as expected. Conditions must be `boolean`; TypeJava does not use
truthiness:

```ts
if (count == 0) {
  console.log("empty");
} else if (count == 1) {
  console.log("one");
} else {
  console.log("many");
}
```

Loops use `while`:

```ts
let i: number = 0;
while (i < values.size()) {
  console.log(values.get(i));
  i = i + 1;
}
```

`break` exits the innermost loop and `continue` starts its next iteration. There is no `for`,
`do while`, or `switch`. Branch and loop bodies must be blocks.

## Operators

Arithmetic operators are `+`, `-`, `*`, `/`, and `%`. Unary operators are `-` and `!`. Comparison
operators are `<`, `<=`, `>`, `>=`, `==`, and `!=`. Logical operators are `&&` and `||` and they
short-circuit.

- `+` is numeric addition when both sides are numbers.
- If either side of `+` is a string, `+` performs concatenation. The other side must be a
  primitive, nullable primitive, or `null`.
- The other arithmetic and ordering operators require non-null numbers.
- Conditions and logical operands must be booleans.
- `==` and `!=` are the only equality operators. `===` and `!==` do not exist.
- There are no assignment operators such as `+=`, no increment/decrement operators, no ternary
  operator, and no bitwise operators.

Parentheses are allowed for grouping. A leading negative number is unary minus, as in `-5`.
Consecutive minus signs such as `a--b` are rejected; use parentheses to make the intent clear.

### Equality semantics

- Numbers compare numerically.
- Booleans compare by value.
- Strings compare by value.
- Nullable values can be compared with `null`.
- Other references use Java equality. Record-bound values use the Java record's value equality.
  Ordinary TypeJava object literals use identity equality because generated object classes do not
  override `equals`.

## Built-ins

### `console.log`

`console.log(values...)` writes one line. Multiple arguments are separated by single spaces.
Whole-number doubles print without a trailing `.0`, `null` prints as `null`, booleans as
`true`/`false`, and strings as their contents.

```ts
console.log("count", 3, true, null);
```

The host controls where output goes; by default it is `System.out`. See the
[implementor guide](IMPLEMENTATORS_GUIDE.md) for custom `Io` implementations.

### `TjArray<T>`

`TjArray` is a mutable, growable array. Bind it with `withStd()` in the environment.

```ts
const names: TjArray<string> = TjArray.create();

names.add("Ada");
names.add("Grace");

console.log(names.size());
console.log(names.get(0));
```

Methods:

- `TjArray.create()` creates an empty array. The destination must be annotated so the element type
  can be inferred.
- `size(): number`
- `get(index: number): T`
- `add(value: T): void`

`get` is an unchecked Java list access: an invalid index throws a Java
`IndexOutOfBoundsException`.

### `TjMap<K, V>`

`TjMap` is a mutable insertion-ordered map. Bind it with `withStd()` in the environment.

```ts
const counts: TjMap<string, number> = TjMap.create();

counts.put("warning", 2);
counts.put("critical", 1);

if (counts.containsKey("warning")) {
  console.log(counts.get("warning"));
}
```

Methods:

- `TjMap.create()` creates an empty map. The destination must be annotated so the key and value
  types can be inferred.
- `size(): number`
- `get(key: K): V`
- `put(key: K, value: V): void`
- `containsKey(key: K): boolean`

`get` returns `null` for a missing key in the runtime, but its declared TypeJava type is `V`.
Check `containsKey` first when absence is possible. Iteration-order-dependent code will see
insertion order because the runtime uses a `LinkedHashMap`.

## Differences from TypeScript

| TypeScript | TypeJava |
|---|---|
| Structural object typing | Nominal object types |
| `undefined`, optional fields, `?` | Only `T \| null`, no optional properties |
| General unions and intersections | Only nullable unions |
| `===` / `!==` | Only `==` / `!=` |
| Truthiness | Conditions must be `boolean` |
| Function values and closures | Function types only as object fields; methods only |
| Classes, inheritance, interfaces | Type aliases and object types |
| Modules and imports | One global source file |
| Exceptions, `try` / `catch` | No language-level exceptions; host exceptions escape |
| `for`, `switch`, ternaries | `while`, `if` / `else`, boolean expressions |
| JavaScript numbers | Every number is a JVM `double` |
| Type inference | Explicit variable and parameter types; factory contexts are the exception |
| Semicolons optional | Semicolons required |
| Array and object spread/literals | `TjArray`, `TjMap`, and contextual object literals |

For Java integration, including exported functions and record-bound types, continue with the
[implementor guide](IMPLEMENTATORS_GUIDE.md).
