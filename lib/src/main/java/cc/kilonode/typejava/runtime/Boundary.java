package cc.kilonode.typejava.runtime;

public final class Boundary {
  private Boundary() {}

  public static void requireNonNull(Object value, String path) {
    if (value == null) {
      throw new IllegalArgumentException(
          "Boundary violation: " + path + " must not be null");
    }
  }
}
