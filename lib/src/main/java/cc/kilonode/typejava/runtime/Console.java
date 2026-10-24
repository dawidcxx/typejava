package cc.kilonode.typejava.runtime;

import cc.kilonode.typejava.Environment;

public final class Console {
  private Console() {}

  public static void log(Environment environment, Object... values) {
    environment.io().println(Values.format(values));
  }
}
