package cc.kilonode.typejava.benchmarks;

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
    Workload workload;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
      workload = count -> JavaLogClassification.run(count);
      warm(workload);
    }
  }

  @State(Scope.Thread)
  public static class TypeJavaState {
    Workload workload;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
      workload =
          TypeJavaLogClassification.compile(
              BenchmarkResources.read("/benchmarks/log-classification.tj"));
      warm(workload);
    }
  }

  @State(Scope.Thread)
  public static class GroovyState {
    Workload workload;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
      workload =
          GroovyLogClassification.compile(
              BenchmarkResources.read("/benchmarks/log-classification.groovy"));
      warm(workload);
    }
  }

  @State(Scope.Thread)
  public static class BeanShellState {
    Workload workload;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
      workload =
          BeanShellLogClassification.compile(
              BenchmarkResources.read("/benchmarks/log-classification.bsh"));
      warm(workload);
    }
  }

  @State(Scope.Thread)
  public static class RhinoState {
    Workload workload;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
      workload =
          RhinoLogClassification.compile(
              BenchmarkResources.read("/benchmarks/log-classification.js"));
      warm(workload);
    }
  }

  @Benchmark
  public String javaBaseline(JavaState state) throws Exception {
    return state.workload.run(LOG_COUNT);
  }

  @Benchmark
  public String typeJava(TypeJavaState state) throws Exception {
    return state.workload.run(LOG_COUNT);
  }

  @Benchmark
  public String groovy(GroovyState state) throws Exception {
    return state.workload.run(LOG_COUNT);
  }

  @Benchmark
  public String beanShell(BeanShellState state) throws Exception {
    return state.workload.run(LOG_COUNT);
  }

  @Benchmark
  public String rhino(RhinoState state) throws Exception {
    return state.workload.run(LOG_COUNT);
  }

  private static void warm(Workload workload) throws Exception {
    for (int i = 0; i < WARMUP_RUNS; i++) {
      workload.run(LOG_COUNT);
    }
  }
}
