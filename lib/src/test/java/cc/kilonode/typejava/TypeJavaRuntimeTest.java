package cc.kilonode.typejava;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.testing.TestFixtures;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TypeJavaRuntimeTest {
  @Test
  void runsHelloWorld() {
    assertEquals("hello world\n", run("hello-world.tj"));
  }

  @Test
  void runsBasicFunctionsAndArithmetic() {
    assertEquals("4\n", run("basic-function.tj"));
    assertEquals("15\n", run("arithmetic.tj"));
  }

  @Test
  void runsBranchesLoopsAndJumps() {
    assertEquals("3\n", run("locals-and-branches.tj"));
    assertEquals("0\n", run("loops.tj"));
    assertEquals("1\n", run("loop-jumps.tj"));
  }

  @Test
  void runsObjectLiteralsAndMutation() {
    assertEquals("1\n", run("object-literals.tj"));
    assertEquals("1\n", run("mutation.tj"));
  }

  @Test
  void runsMethodsAndNullableTypes() {
    assertEquals("Hi, I'm Alice\n", run("person.tj"));
    assertEquals("30\nnull\n", run("unions.tj"));
  }

  @Test
  void runsCommentsStringsAndGenerics() {
    assertEquals("done\n", run("comments.tj"));
    assertEquals("123 12.5 hello \n", run("numbers-and-strings.tj"));
    assertEquals("billing\n", run("generics.tj"));
  }

  @Test
  void runsRecursiveAliasesThatCrossObjectFields() {
    String source =
        """
        type NodeAlias = Node;
        type Node = {
          value: number;
          next: NodeAlias | null;
        };

        const node: Node = { value: 8, next: null };
        console.log(node.value);
        """;
    assertEquals("8\n", runSource(source));
  }

  @Test
  void runsInlineObjectTypeParameters() {
    String source =
        """
        function use(point: { x: number; }): number {
          return point.x + 1;
        };

        console.log(use({ x: 4 }));
        """;
    assertEquals("5\n", runSource(source));
  }

  @Test
  void methodsCanReadTopLevelVariables() {
    String source =
        """
        const prefix: string = "hi";

        type Greeter = {
          greet: (this: Greeter) => string;
        };

        const greeter: Greeter = {
          greet(this: Greeter): string {
            return prefix + "!";
          }
        };

        console.log(greeter.greet());
        """;
    assertEquals("hi!\n", runSource(source));
  }

  @Test
  void runsGenericInlineObjectTypes() {
    String source =
        """
        type G<T> = {
          inner: { value: T; };
        };

        const g: G<number> = { inner: { value: 3 } };
        console.log(g.inner.value);
        """;
    assertEquals("3\n", runSource(source));
  }

  @Test
  void boxesGenericMethodParametersAndReturns() {
    String source =
        """
        type Fn<T> = {
          apply: (this: Fn<T>, value: T) => T;
        };

        const increment: Fn<number> = {
          apply(this: Fn<number>, value: number): number {
            return value + 1;
          }
        };

        console.log(increment.apply(4));
        """;
    assertEquals("5\n", runSource(source));
  }

  @Test
  void runsRecursiveNullableObjectFields() {
    String source =
        """
        type Node = {
          value: number;
          next: Node | null;
        };

        const second: Node = { value: 2, next: null };
        const first: Node = { value: 1, next: second };

        if (first.next != null) {
          console.log(first.next.value);
        }
        """;
    assertEquals("2\n", runSource(source));
  }

  @Test
  void dispatchesMethodsPerLiteralSite() {
    String source =
        """
        type Counter = {
          value: number;
          inc: (this: Counter) => number;
        };

        type Adder = {
          add: (value: number) => number;
        };

        const a: Counter = {
          value: 1,
          inc(this: Counter): number {
            return this.value + 1;
          }
        };

        const b: Counter = {
          value: 10,
          inc(this: Counter): number {
            return this.value + 2;
          }
        };

        const adder: Adder = {
          add(value: number): number {
            return value + 1;
          }
        };

        console.log(a.inc(), b.inc(), adder.add(4));
        """;
    assertEquals("2 12 5\n", runSource(source));
  }

  @Test
  void runsReadmeExample() {
    assertEquals(
        """
        billing/critical -> page
        auth/warning -> ticket
        cron/unknown -> watch
        first critical: billing
        boxed service: billing
        classified 3, critical 1
        """,
        run("readme.tj"));
  }

  private static String run(String fixture) {
    return runSource(TestFixtures.read(fixture));
  }

  private static String runSource(String source) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    TypeJava.run(source, new PrintStream(bytes, true, StandardCharsets.UTF_8));
    return bytes.toString(StandardCharsets.UTF_8);
  }
}
