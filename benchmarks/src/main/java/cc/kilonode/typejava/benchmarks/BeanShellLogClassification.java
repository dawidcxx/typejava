package cc.kilonode.typejava.benchmarks;

import bsh.EvalError;
import bsh.Interpreter;

final class BeanShellLogClassification {
  private BeanShellLogClassification() {}

  static Workload compile(String source) {
    Interpreter interpreter = new Interpreter();

    try {
      interpreter.eval(source);
    } catch (EvalError error) {
      throw new IllegalStateException("Could not compile BeanShell workload", error);
    }

    return count ->
        (String)
            interpreter
                .getNameSpace()
                .invokeMethod("runWorkload", new Object[] {count}, interpreter);
  }
}
