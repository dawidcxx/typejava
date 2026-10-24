package cc.kilonode.typejava.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
  @Test
  void reportsUsageForWrongArgumentCount() {
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    int exitCode = Main.execute(new String[0], sink(), stream(err));

    assertEquals(2, exitCode);
    assertTrue(err.toString(StandardCharsets.UTF_8).contains("Usage: typejava <script.tj>"));
  }

  @Test
  void runsScriptFile(@TempDir Path directory) throws IOException {
    Path script = directory.resolve("hello.tj");
    Files.writeString(script, "console.log(\"hello from cli\");", StandardCharsets.UTF_8);
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    int exitCode = Main.execute(new String[] {script.toString()}, stream(out), sink());

    assertEquals(0, exitCode);
    assertEquals("hello from cli\n", out.toString(StandardCharsets.UTF_8));
  }

  @Test
  void reportsMissingScript(@TempDir Path directory) {
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    int exitCode =
        Main.execute(
            new String[] {directory.resolve("missing.tj").toString()}, sink(), stream(err));

    assertEquals(1, exitCode);
    assertTrue(err.toString(StandardCharsets.UTF_8).startsWith("error: "));
  }

  private static PrintStream stream(ByteArrayOutputStream bytes) {
    return new PrintStream(bytes, true, StandardCharsets.UTF_8);
  }

  private static PrintStream sink() {
    return stream(new ByteArrayOutputStream());
  }
}
