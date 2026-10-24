package cc.kilonode.typejava;

public final class DefaultIo implements Io {
  @Override
  public void println(String line) {
    System.out.println(line);
  }
}
