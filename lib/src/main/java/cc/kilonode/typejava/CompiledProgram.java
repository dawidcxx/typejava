package cc.kilonode.typejava;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A compiled TypeJava program that can be executed repeatedly without recompiling.
 *
 * <p>Instances are created by {@link TypeJava#compile(String, Environment)}. A program is not
 * thread-safe; run it from one thread at a time.
 */
public final class CompiledProgram {
  private final Method run;
  private final Environment environment;

  CompiledProgram(Method run, Environment environment) {
    this.run = run;
    this.environment = environment;
  }

  /** Returns the environment captured when this program was compiled. */
  public Environment environment() {
    return environment;
  }

  /** Runs the program. */
  public void run() {
    try {
      run.invoke(null, environment);
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
    }
  }
}
