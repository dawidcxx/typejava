package cc.kilonode.typejava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cc.kilonode.typejava.testing.TestFixtures;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TypeJavaCheckerTest {
  @Test
  void rejectsUnknownVariablesAndFunctions() {
    assertError("console.log(missing);", "Unknown variable 'missing'");
    assertError("missing();", "Unknown function 'missing'");
  }

  @Test
  void rejectsTypeMismatches() {
    assertError("let n: number = \"nope\";", "Expected number but found string");
    assertError("let n: number = null;", "Expected number but found null");
    assertError("if (1) { console.log(\"x\"); }", "Expected boolean but found number");
    assertError("let n: number = 1 + true;", "Operator '+' requires two numbers");
  }

  @Test
  void rejectsInvalidAssignments() {
    assertError("const n: number = 1; n = 2;", "Cannot assign to constant 'n'");
    assertError(
        "type P = { readonly x: number; }; const p: P = { x: 1 }; p.x = 2;",
        "Cannot assign to readonly field 'x'");
  }

  @Test
  void rejectsInvalidObjectLiterals() {
    assertError("type P = { x: number; }; const p: P = {};", "Missing field 'x'");
    assertError(
        "type P = { x: number; }; const p: P = { x: 1, y: 2 };",
        "Unknown field 'y'");
    assertError("console.log({ x: 1 });", "Object literals require a nominal contextual type");
  }

  @Test
  void rejectsNullableAccessWithoutNarrowing() {
    assertError(
        "type P = { x: number; }; const p: P | null = null; console.log(p.x);",
        "Cannot access a member on a nullable value");
  }

  @Test
  void rejectsInvalidControlFlow() {
    assertError("break;", "'break' is only allowed inside a loop");
    assertError("return 1;", "'return' is not allowed at the top level");
    assertError(
        "function f(): number { let x: number = 1; };",
        "Not all code paths return a value");
  }

  @Test
  void rejectsFunctionValuesAndBadMethods() {
    assertError(
        "function f(): number { return 1; }; const g: number = f;",
        "Function 'f' is not a value");
    assertError(
        "type P = { m: (this: P) => number; }; const p: P = { m(): number { return 1; } };",
        "must declare this");
    assertError(
        "type P = { m: (this: P) => number; }; const p: P = { m(this: P): string { return \"x\"; } };",
        "Method returns string but number is required");
  }

  @Test
  void rejectsBadGenericAndBuiltinUsage() {
    assertError(
        "type Box<T> = { value: T; }; const b: Box = { value: 1 };",
        "expects 1 type argument");
    assertError(
        "const xs: TjArray<number> = TjArray.create(1);",
        "does not take arguments");
    assertError(
        "console.log(TjArray.create());",
        "Cannot infer the type of TjArray.create()");
    assertError(
        "const xs: TjArray<number> = TjArray.create(); xs.add(\"x\");",
        "Expected number but found string");
  }

  @Test
  void acceptsEquivalentDuplicateTypeDeclarationsInAnyFieldOrder() {
    String source =
        """
        type P = { a: number; b: string; };
        type P = { b: string; a: number; };
        const p: P = { a: 1, b: "x" };
        console.log(p.a, p.b);
        """;
    assertEquals("1 x\n", run(source));
    assertError(
        "type P = { a: number; }; type P = { a: string; };",
        "already declared with a different definition");
  }

  @Test
  void acceptsNarrowingAndGenericMethods() {
    String source =
        """
        type Box<T> = {
          value: T;
          get: (this: Box<T>) => T;
        };

        type Outer = { inner: { value: number; }; };

        const box: Box<number> = {
          value: 4,
          get(this: Box<number>): number {
            return this.value + 1;
          }
        };

        const outer: Outer = { inner: { value: 7 } };
        const maybe: Box<number> | null = box;

        if (maybe != null) {
          console.log(box.get(), outer.inner.value, maybe.value);
        }
        """;
    assertEquals("5 7 4\n", run(source));
  }

  private static void assertError(String source, String message) {
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> TypeJava.run(source, TestFixtures.environment(sink())));
    assertTrue(
        exception.getMessage().contains(message),
        () -> "Expected message containing '" + message + "' but got: " + exception.getMessage());
  }

  private static String run(String source) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
    TypeJava.run(source, TestFixtures.environment(out));
    return bytes.toString(StandardCharsets.UTF_8);
  }

  private static PrintStream sink() {
    return new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
  }
}
