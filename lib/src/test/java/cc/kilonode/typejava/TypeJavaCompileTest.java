package cc.kilonode.typejava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cc.kilonode.typejava.checker.CheckException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TypeJavaCompileTest {
  @Test
  void compilesWithoutRunningAndRunsRepeatedly() {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);

    CompiledProgram program = TypeJava.compile("console.log(\"hello\");");
    assertEquals("", bytes.toString(StandardCharsets.UTF_8));

    program.run(out);
    program.run(out);
    assertEquals(
        ("hello" + System.lineSeparator()).repeat(2),
        bytes.toString(StandardCharsets.UTF_8));
  }

  @Test
  void reportsCompileErrorsBeforeRunning() {
    assertThrows(CheckException.class, () -> TypeJava.compile("console.log(missing);"));
  }
}
