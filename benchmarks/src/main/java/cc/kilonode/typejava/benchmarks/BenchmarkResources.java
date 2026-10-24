package cc.kilonode.typejava.benchmarks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

final class BenchmarkResources {
  private BenchmarkResources() {}

  static String read(String resource) {
    try (InputStream input = BenchmarkResources.class.getResourceAsStream(resource)) {
      if (input == null) {
        throw new IllegalStateException("Missing benchmark resource " + resource);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
