package cc.kilonode.typejava.runtime;

import java.io.PrintStream;

public final class Console {
  private static final ThreadLocal<PrintStream> OUT =
      ThreadLocal.withInitial(() -> System.out);

  private Console() {}

  public static void log(Object... values) {
    StringBuilder line = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        line.append(' ');
      }
      line.append(Values.toDisplayString(values[i]));
    }
    OUT.get().println(line);
  }

  public static PrintStream currentOut() {
    return OUT.get();
  }

  public static void setOut(PrintStream out) {
    OUT.set(out);
  }

  public static void clearOut() {
    OUT.remove();
  }
}
