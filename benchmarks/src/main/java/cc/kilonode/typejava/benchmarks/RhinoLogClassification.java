package cc.kilonode.typejava.benchmarks;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

final class RhinoLogClassification {
  private RhinoLogClassification() {}

  static Workload compile(String source) {
    Context context = Context.enter();

    try {
      Scriptable scope = context.initStandardObjects();
      Script script = context.compileString(source, "LogClassification.js", 1, null);
      script.exec(context, scope, scope);

      Object workload = ScriptableObject.getProperty(scope, "runWorkload");
      if (!(workload instanceof Function function)) {
        throw new IllegalStateException("Rhino workload is missing runWorkload");
      }

      return (count, out) -> {
        Context runContext = Context.enter();
        try {
          function.call(runContext, scope, scope, new Object[] {count, out});
        } finally {
          Context.exit();
        }
      };
    } finally {
      Context.exit();
    }
  }
}
