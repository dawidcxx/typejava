package cc.kilonode.typejava.benchmarks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JavaLogClassification {
  private static final String[] SERVICES = {"billing", "auth", "cron", "search", "checkout"};
  private static final String[] MESSAGES = {
    "timeout", "card declined", "login failed", "nothing to report"
  };

  private JavaLogClassification() {}

  static String run(int count) {
    List<UpstreamLog> logs = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      logs.add(
          new UpstreamLog(
              SERVICES[i % SERVICES.length], MESSAGES[i % MESSAGES.length], levelAt(i)));
    }

    Classifier classifier = new Classifier();
    Map<String, Integer> severityCounts = new LinkedHashMap<>();
    Map<String, Integer> actionCounts = new LinkedHashMap<>();
    int critical = 0;
    int pageable = 0;

    for (UpstreamLog log : logs) {
      Classification result = classifier.classify(log);
      increment(severityCounts, result.severity());
      increment(actionCounts, result.action());

      if (result.severity().equals("critical")) {
        critical++;
      }

      if (result.action().equals("page")) {
        pageable++;
      }
    }

    int warnings = severityCounts.getOrDefault("warning", 0);
    return "logs="
        + logs.size()
        + " critical="
        + critical
        + " warnings="
        + warnings
        + " page="
        + pageable
        + " severities="
        + severityCounts.size()
        + " actions="
        + actionCounts.size();
  }

  private static Double levelAt(int index) {
    int slot = index % 7;

    if (slot == 0) {
      return null;
    }

    if (slot == 6) {
      return 5.0;
    }

    return (double) slot;
  }

  private static void increment(Map<String, Integer> counts, String key) {
    Integer current = counts.get(key);

    if (current == null) {
      counts.put(key, 1);
    } else {
      counts.put(key, current + 1);
    }
  }

  private static final class Classifier {
    Classification classify(UpstreamLog log) {
      String severity = severityOf(log.level());
      double priority = basePriority(severity, log.service());
      String action = actionOf(severity, priority, log.message());
      return new Classification(log.service(), severity, action, priority);
    }

    private static String severityOf(Double level) {
      if (level == null) {
        return "unknown";
      }

      if (level >= 5) {
        return "critical";
      }

      if (level >= 3) {
        return "warning";
      }

      return "info";
    }

    private static double basePriority(String severity, String service) {
      if (severity.equals("critical")) {
        return 3;
      }

      if (severity.equals("warning")) {
        if (service.equals("billing")) {
          return 2;
        }

        return 1;
      }

      if (severity.equals("info")) {
        return 0;
      }

      return -1;
    }

    private static String actionOf(String severity, double priority, String message) {
      if (message.equals("card declined")) {
        return "page";
      }

      if (severity.equals("critical")) {
        return "page";
      }

      if (severity.equals("warning") && priority >= 2) {
        return "page";
      }

      if (severity.equals("warning")) {
        return "ticket";
      }

      if (severity.equals("info")) {
        return "log";
      }

      return "ignore";
    }
  }

  private record UpstreamLog(String service, String message, Double level) {}

  private record Classification(String service, String severity, String action, double priority) {}
}
