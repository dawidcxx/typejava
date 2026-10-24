package cc.kilonode.typejava;

public interface Io {
  default void println(String line) {
    throw new UnsupportedOperationException("println");
  }
}
