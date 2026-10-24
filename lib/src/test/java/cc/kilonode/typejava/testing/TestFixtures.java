package cc.kilonode.typejava.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class TestFixtures {
  private static final Path ROOT = Path.of("src/test/resources/fixtures");

  private TestFixtures() {}

  public static Path root() {
    return ROOT;
  }

  public static Path path(String name) {
    return ROOT.resolve(name);
  }

  public static String read(String name) {
    try {
      return Files.readString(path(name));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static List<String> names(String extension) {
    try (Stream<Path> paths = Files.list(ROOT)) {
      return paths
          .map(path -> path.getFileName().toString())
          .filter(name -> name.endsWith(extension))
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
