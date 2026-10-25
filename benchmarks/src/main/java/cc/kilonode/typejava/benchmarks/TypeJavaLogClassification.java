package cc.kilonode.typejava.benchmarks;

import cc.kilonode.typejava.CompiledProgram;
import cc.kilonode.typejava.Environment;
import cc.kilonode.typejava.ProgramInstance;
import cc.kilonode.typejava.TypeJava;

public final class TypeJavaLogClassification {
  public interface Api {
    String runWorkload(double count);
  }

  private TypeJavaLogClassification() {}

  static Workload compile(String source) {
    CompiledProgram program = TypeJava.compile(source, Environment.standard());
    ProgramInstance instance = program.instantiate();
    Api api = instance.exports(Api.class);
    return count -> api.runWorkload(count);
  }
}
