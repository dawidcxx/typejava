package cc.kilonode.typejava.benchmarks;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class GroovyLogClassification {
  private GroovyLogClassification() {}

  static Workload compile(String source) {
    GroovyShell shell = new GroovyShell(GroovyLogClassification.class.getClassLoader());
    Script script = shell.parse(source, "LogClassification.groovy");

    try {
      Method method = script.getClass().getMethod("runWorkload", int.class);
      return count -> {
        try {
          return (String) method.invoke(script, count);
        } catch (InvocationTargetException exception) {
          Throwable cause = exception.getCause();
          if (cause instanceof Exception checked) {
            throw checked;
          }
          if (cause instanceof Error error) {
            throw error;
          }
          throw new RuntimeException(cause);
        }
      };
    } catch (NoSuchMethodException exception) {
      throw new IllegalStateException("Groovy workload is missing runWorkload", exception);
    }
  }
}
