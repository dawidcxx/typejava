package cc.kilonode.typejava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cc.kilonode.typejava.checker.CheckException;
import cc.kilonode.typejava.testing.TestFixtures;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypeJavaCompileTest {
  @Test
  void compilesWithoutRunningAndRunsRepeatedly() {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);

    CompiledProgram program =
        TypeJava.compile("console.log(\"hello\");", TestFixtures.environment(out));
    assertEquals("", bytes.toString(StandardCharsets.UTF_8));

    program.run();
    program.run();
    assertEquals(
        ("hello" + System.lineSeparator()).repeat(2),
        bytes.toString(StandardCharsets.UTF_8));
  }

  @Test
  void reportsCompileErrorsBeforeRunning() {
    assertThrows(
        CheckException.class,
        () -> TypeJava.compile("console.log(missing);", TestFixtures.environment()));
  }

  @Test
  void checksWithoutRunning() {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);

    List<Diagnostic> diagnostics =
        TypeJava.check("console.log(\"hello\");", TestFixtures.environment(out));

    assertEquals(List.of(), diagnostics);
    assertEquals("", bytes.toString(StandardCharsets.UTF_8));
  }

  @Test
  void checkReportsFirstDiagnostic() {
    List<Diagnostic> diagnostics =
        TypeJava.check("console.log(missing);", TestFixtures.environment());

    assertEquals(1, diagnostics.size());
    Diagnostic diagnostic = diagnostics.getFirst();
    assertEquals(1, diagnostic.line());
    assertTrue(diagnostic.column() > 0);
    assertTrue(diagnostic.message().contains("missing"));
  }
}
