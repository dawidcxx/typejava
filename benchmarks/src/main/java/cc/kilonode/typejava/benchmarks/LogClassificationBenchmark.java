package cc.kilonode.typejava.benchmarks;

import cc.kilonode.typejava.CompiledProgram;
import cc.kilonode.typejava.TypeJava;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class LogClassificationBenchmark {
  static final int LOG_COUNT = 10_000;

  private static final int WARMUP_RUNS = Integer.getInteger("tj.benchmark.warmups", 1_000);

  @State(Scope.Thread)
  public static class JavaState {
    PrintStream sink;
    Workload workload;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
      sink = sink();
      workload = (count, out) -> JavaLogClassification.run(count, out);
      warm(workload, sink);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
      sink.close();
    }
  }

  @State(Scope.Thread)
  public static class TypeJavaState {
    PrintStream sink;
    Workload workload;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
      sink = sink();
      CompiledProgram program =
          TypeJava.compile(BenchmarkResources.read("/benchmarks/log-classification.tj"));
      workload = (count, out) -> program.run(out);
      warm(workload, sink);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
      sink.close();
    }
  }

  @State(Scope.Thread)
  public static class GroovyState {
    PrintStream sink;
    Workload workload;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
      sink = sink();
      workload =
          GroovyLogClassification.compile(
              BenchmarkResources.read("/benchmarks/log-classification.groovy"));
      warm(workload, sink);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
      sink.close();
    }
  }

  @State(Scope.Thread)
  public static class BeanShellState {
    PrintStream sink;
    Workload workload;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
      sink = sink();
      workload =
          BeanShellLogClassification.compile(
              BenchmarkResources.read("/benchmarks/log-classification.bsh"));
      warm(workload, sink);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
      sink.close();
    }
  }

  @State(Scope.Thread)
  public static class RhinoState {
    PrintStream sink;
    Workload workload;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
      sink = sink();
      workload =
          RhinoLogClassification.compile(
              BenchmarkResources.read("/benchmarks/log-classification.js"));
      warm(workload, sink);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
      sink.close();
    }
  }

  @Benchmark
  public void javaBaseline(JavaState state) throws Exception {
    state.workload.run(LOG_COUNT, state.sink);
  }

  @Benchmark
  public void typeJava(TypeJavaState state) throws Exception {
    state.workload.run(LOG_COUNT, state.sink);
  }

  @Benchmark
  public void groovy(GroovyState state) throws Exception {
    state.workload.run(LOG_COUNT, state.sink);
  }

  @Benchmark
  public void beanShell(BeanShellState state) throws Exception {
    state.workload.run(LOG_COUNT, state.sink);
  }

  @Benchmark
  public void rhino(RhinoState state) throws Exception {
    state.workload.run(LOG_COUNT, state.sink);
  }

  private static PrintStream sink() {
    return new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);
  }

  private static void warm(Workload workload, PrintStream sink) throws Exception {
    for (int i = 0; i < WARMUP_RUNS; i++) {
      workload.run(LOG_COUNT, sink);
    }
  }
}
