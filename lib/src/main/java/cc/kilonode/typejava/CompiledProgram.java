package cc.kilonode.typejava;

import cc.kilonode.typejava.runtime.Console;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A compiled TypeJava program that can be executed repeatedly without recompiling.
 *
 * <p>Instances are created by {@link TypeJava#compile(String)}. A program is not thread-safe; run
 * it from one thread at a time.
 */
public final class CompiledProgram {
  private final Method main;

  CompiledProgram(Method main) {
    this.main = main;
  }

  /** Runs the program, writing console output to {@link System#out}. */
  public void run() {
    run(System.out);
  }

  /**
   * Runs the program.
   *
   * @param out destination for {@code console.log}
   * @param args arguments passed to the generated main method
   */
  public void run(PrintStream out, String... args) {
    PrintStream previous = Console.currentOut();
    Console.setOut(out);
    try {
      main.invoke(null, (Object) args);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new RuntimeException(cause);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Could not start generated TypeJava program", exception);
    } finally {
      Console.setOut(previous);
    }
  }
}
