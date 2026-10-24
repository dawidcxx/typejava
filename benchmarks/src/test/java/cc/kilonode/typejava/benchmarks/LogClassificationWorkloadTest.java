package cc.kilonode.typejava.benchmarks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.CompiledProgram;
import cc.kilonode.typejava.Environment;
import cc.kilonode.typejava.Io;
import cc.kilonode.typejava.TypeJava;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LogClassificationWorkloadTest {
  private static final String TJ = "/benchmarks/log-classification.tj";
  private static final String GROOVY = "/benchmarks/log-classification.groovy";
  private static final String BEANSHELL = "/benchmarks/log-classification.bsh";
  private static final String RHINO = "/benchmarks/log-classification.js";

  @Test
  void allImplementationsProduceTheSameSummary() throws Exception {
    String expected = run((count, out) -> JavaLogClassification.run(count, out));

    assertEquals(expected, runTypeJava(), "typejava");
    assertEquals(expected, run(GroovyLogClassification.compile(source(GROOVY))), "groovy");
    assertEquals(
        expected, run(BeanShellLogClassification.compile(source(BEANSHELL))), "beanshell");
    assertEquals(expected, run(RhinoLogClassification.compile(source(RHINO))), "rhino");
  }

  @Test
  void compiledProgramRunsRepeatedly() {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
    CompiledProgram program = TypeJava.compile(source(TJ), environment(out));

    program.run();
    String first = bytes.toString(StandardCharsets.UTF_8);
    bytes.reset();
    program.run();
    assertEquals(first, bytes.toString(StandardCharsets.UTF_8));
  }

  private static String runTypeJava() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
    CompiledProgram program = TypeJava.compile(source(TJ), environment(out));
    program.run();
    return bytes.toString(StandardCharsets.UTF_8).stripTrailing();
  }

  private static Environment environment(PrintStream out) {
    return TypeJava.environment()
        .io(
            new Io() {
              @Override
              public void println(String line) {
                out.println(line);
              }
            })
        .withStd()
        .build();
  }

  private static String run(Workload workload) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    workload.run(
        LogClassificationBenchmark.LOG_COUNT,
        new PrintStream(bytes, true, StandardCharsets.UTF_8));
    return bytes.toString(StandardCharsets.UTF_8).stripTrailing();
  }

  private static String source(String resource) {
    return BenchmarkResources.read(resource);
  }
}
