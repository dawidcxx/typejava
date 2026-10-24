package cc.kilonode.typejava.testing;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class SnapshotAssertions {
  private static final Path ACTUAL_ROOT = Path.of("build/snapshot-actual");

  private SnapshotAssertions() {}

  public static void assertMatches(Path snapshot, String actual) {
    try {
      if (isUpdateMode()) {
        Files.createDirectories(snapshot.getParent());
        Files.writeString(
            snapshot,
            actual,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);
        return;
      }

      if (!Files.exists(snapshot)) {
        fail("Missing snapshot: " + snapshot + ". Run with UPDATE_SNAPSHOTS=true to create it.");
      }

      String expected = normalize(Files.readString(snapshot, StandardCharsets.UTF_8));
      String normalizedActual = normalize(actual);
      if (!expected.equals(normalizedActual)) {
        Path actualPath = writeActual(snapshot, normalizedActual);
        fail(
            "Snapshot mismatch: "
                + snapshot
                + "\n"
                + firstDifference(expected, normalizedActual)
                + "\nActual snapshot written to: "
                + actualPath
                + "\nRun with UPDATE_SNAPSHOTS=true to update.");
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static boolean isUpdateMode() {
    return Boolean.parseBoolean(System.getProperty("typejava.updateSnapshots"))
        || "true".equalsIgnoreCase(System.getenv("UPDATE_SNAPSHOTS"));
  }

  private static String normalize(String value) {
    return value.replace("\r\n", "\n");
  }

  private static Path writeActual(Path snapshot, String actual) throws IOException {
    Path actualPath = ACTUAL_ROOT.resolve(snapshot);
    Files.createDirectories(actualPath.getParent());
    Files.writeString(
        actualPath,
        actual,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
    return actualPath;
  }

  private static String firstDifference(String expected, String actual) {
    String[] expectedLines = expected.split("\n", -1);
    String[] actualLines = actual.split("\n", -1);
    int lines = Math.max(expectedLines.length, actualLines.length);

    for (int i = 0; i < lines; i++) {
      String expectedLine = i < expectedLines.length ? expectedLines[i] : "<missing>";
      String actualLine = i < actualLines.length ? actualLines[i] : "<missing>";
      if (!expectedLine.equals(actualLine)) {
        return "First difference at line "
            + (i + 1)
            + ":\n  expected: "
            + expectedLine
            + "\n  actual:   "
            + actualLine;
      }
    }

    return "Snapshots differ only in trailing newlines.";
  }
}
