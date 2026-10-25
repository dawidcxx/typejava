package cc.kilonode.typejava.benchmarks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.CompiledProgram;
import cc.kilonode.typejava.Environment;
import cc.kilonode.typejava.ProgramInstance;
import cc.kilonode.typejava.TypeJava;
import org.junit.jupiter.api.Test;

class LogClassificationWorkloadTest {
  private static final String TJ = "/benchmarks/log-classification.tj";
  private static final String GROOVY = "/benchmarks/log-classification.groovy";
  private static final String BEANSHELL = "/benchmarks/log-classification.bsh";
  private static final String RHINO = "/benchmarks/log-classification.js";

  @Test
  void allImplementationsProduceTheSameSummary() throws Exception {
    String expected = JavaLogClassification.run(LogClassificationBenchmark.LOG_COUNT);

    assertEquals(expected, runTypeJava(), "typejava");
    assertEquals(expected, run(GroovyLogClassification.compile(source(GROOVY))), "groovy");
    assertEquals(
        expected, run(BeanShellLogClassification.compile(source(BEANSHELL))), "beanshell");
    assertEquals(expected, run(RhinoLogClassification.compile(source(RHINO))), "rhino");
  }

  @Test
  void compiledProgramRunsExportedFunctionRepeatedly() {
    Environment environment = Environment.standard();
    CompiledProgram program = TypeJava.compile(source(TJ), environment);
    ProgramInstance instance = program.instantiate();
    TypeJavaLogClassification.Api api =
        instance.exports(TypeJavaLogClassification.Api.class);

    String first = api.runWorkload(LogClassificationBenchmark.LOG_COUNT);
    String second = api.runWorkload(LogClassificationBenchmark.LOG_COUNT);
    assertEquals(first, second);
  }

  private static String runTypeJava() throws Exception {
    return TypeJavaLogClassification.compile(source(TJ))
        .run(LogClassificationBenchmark.LOG_COUNT);
  }

  private static String run(Workload workload) throws Exception {
    return workload.run(LogClassificationBenchmark.LOG_COUNT);
  }

  private static String source(String resource) {
    return BenchmarkResources.read(resource);
  }
}
