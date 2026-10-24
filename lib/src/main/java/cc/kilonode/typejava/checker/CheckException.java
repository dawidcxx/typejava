package cc.kilonode.typejava.checker;

public final class CheckException extends RuntimeException {
  private final int line;
  private final int column;

  public CheckException(String message, int line, int column) {
    super(message + " at line " + line + ", column " + column);
    this.line = line;
    this.column = column;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }
}
