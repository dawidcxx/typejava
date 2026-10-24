import java.io.PrintStream

String severityOf(Integer level) {
  if (level == null) {
    return 'unknown'
  }

  if (level >= 5) {
    return 'critical'
  }

  if (level >= 3) {
    return 'warning'
  }

  'info'
}

double basePriority(String severity, String service) {
  if (severity == 'critical') {
    return 3
  }

  if (severity == 'warning') {
    return service == 'billing' ? 2 : 1
  }

  severity == 'info' ? 0 : -1
}

String actionOf(String severity, double priority, String message) {
  if (message == 'card declined') {
    return 'page'
  }

  if (severity == 'critical') {
    return 'page'
  }

  if (severity == 'warning' && priority >= 2) {
    return 'page'
  }

  if (severity == 'warning') {
    return 'ticket'
  }

  if (severity == 'info') {
    return 'log'
  }

  'ignore'
}

String serviceAt(int index) {
  def slot = index % 5

  if (slot == 0) {
    return 'billing'
  }

  if (slot == 1) {
    return 'auth'
  }

  if (slot == 2) {
    return 'cron'
  }

  if (slot == 3) {
    return 'search'
  }

  'checkout'
}

Integer levelAt(int index) {
  def slot = index % 7

  if (slot == 0) {
    return null
  }

  if (slot == 6) {
    return 5
  }

  slot
}

String messageAt(int index) {
  def slot = index % 4

  if (slot == 0) {
    return 'timeout'
  }

  if (slot == 1) {
    return 'card declined'
  }

  if (slot == 2) {
    return 'login failed'
  }

  'nothing to report'
}

Map classify(Map log) {
  def severity = severityOf(log.level as Integer)
  def priority = basePriority(severity, log.service as String)
  def action = actionOf(severity, priority, log.message as String)

  [service: log.service, severity: severity, action: action, priority: priority]
}

String runWorkload(int count, PrintStream out) {
  def logs = new ArrayList(count)

  for (int i = 0; i < count; i++) {
    logs << [service: serviceAt(i), message: messageAt(i), level: levelAt(i)]
  }

  def severityCounts = new LinkedHashMap<String, Integer>()
  def actionCounts = new LinkedHashMap<String, Integer>()
  int critical = 0
  int pageable = 0

  for (log in logs) {
    def result = classify(log as Map)
    severityCounts[result.severity] = (severityCounts[result.severity] ?: 0) + 1
    actionCounts[result.action] = (actionCounts[result.action] ?: 0) + 1

    if (result.severity == 'critical') {
      critical++
    }

    if (result.action == 'page') {
      pageable++
    }
  }

  int warnings = severityCounts['warning'] ?: 0
  String summary = "logs=${logs.size()} critical=$critical warnings=$warnings page=$pageable " +
      "severities=${severityCounts.size()} actions=${actionCounts.size()}"
  out.println(summary)
  summary
}
