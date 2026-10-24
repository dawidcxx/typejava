package cc.kilonode.typejava.benchmarks;

import java.io.PrintStream;

interface Workload {
  void run(int count, PrintStream out) throws Exception;
}
