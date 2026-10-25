package cc.kilonode.typejava;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A compiled TypeJava program that can be instantiated repeatedly without recompiling.
 *
 * <p>The compiled program is immutable and may be shared between threads. Each {@link
 * #instantiate()} call creates isolated top-level state. A {@link ProgramInstance} is not
 * thread-safe.
 */
public final class CompiledProgram {
  private final Constructor<?> constructor;
  private final Method run;
  private final Environment environment;
  private final Map<String, ExportedFunction> exports;

  CompiledProgram(
      Constructor<?> constructor,
      Method run,
      Environment environment,
      Map<String, ExportedFunction> exports) {
    this.constructor = constructor;
    this.run = run;
    this.environment = environment;
    this.exports = Map.copyOf(exports);
  }

  public Environment environment() {
    return environment;
  }

  public Map<String, ExportSignature> exports() {
    Map<String, ExportSignature> signatures = new LinkedHashMap<>();
    for (ExportedFunction function : exports.values()) {
      signatures.put(function.name(), function.signature());
    }
    return Map.copyOf(signatures);
  }

  public ProgramInstance instantiate() {
    try {
      Object target = constructor.newInstance();
      run.invoke(target, environment);
      return new ProgramInstance(target, environment, exports);
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
      throw new IllegalStateException("Could not instantiate generated TypeJava program", exception);
    }
  }

  public void run() {
    instantiate();
  }
}
