package cc.kilonode.typejava.examples;

import cc.kilonode.typejava.TypeJava;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class Main {
  private static final List<String> EXAMPLES = List.of("hello", "objects", "generics");

  private Main() {}

  public static void main(String[] args) {
    List<String> names = args.length == 0 ? EXAMPLES : List.of(args);

    for (String name : names) {
      run(name);
    }
  }

  private static void run(String name) {
    String resource = "/examples/" + name + ".tj";

    try (InputStream input = Main.class.getResourceAsStream(resource)) {
      if (input == null) {
        throw new IllegalArgumentException(
            "Unknown example '" + name + "'. Available: " + String.join(", ", EXAMPLES));
      }

      String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      System.out.println("=== " + name + " ===");
      TypeJava.run(source, System.out);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
