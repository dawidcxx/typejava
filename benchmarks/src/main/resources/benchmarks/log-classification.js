function severityOf(level) {
  if (level === null) {
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

function basePriority(severity, service) {
  if (severity === "critical") {
    return 3;
  }

  if (severity === "warning") {
    return service === "billing" ? 2 : 1;
  }

  if (severity === "info") {
    return 0;
  }

  return -1;
}

function actionOf(severity, priority, message) {
  if (message === "card declined") {
    return "page";
  }

  if (severity === "critical") {
    return "page";
  }

  if (severity === "warning" && priority >= 2) {
    return "page";
  }

  if (severity === "warning") {
    return "ticket";
  }

  if (severity === "info") {
    return "log";
  }

  return "ignore";
}

function serviceAt(index) {
  var slot = index % 5;

  if (slot === 0) {
    return "billing";
  }

  if (slot === 1) {
    return "auth";
  }

  if (slot === 2) {
    return "cron";
  }

  if (slot === 3) {
    return "search";
  }

  return "checkout";
}

function levelAt(index) {
  var slot = index % 7;

  if (slot === 0) {
    return null;
  }

  if (slot === 6) {
    return 5;
  }

  return slot;
}

function messageAt(index) {
  var slot = index % 4;

  if (slot === 0) {
    return "timeout";
  }

  if (slot === 1) {
    return "card declined";
  }

  if (slot === 2) {
    return "login failed";
  }

  return "nothing to report";
}

function classify(log) {
  var severity = severityOf(log.level);
  var priority = basePriority(severity, log.service);
  var action = actionOf(severity, priority, log.message);

  return {
    service: log.service,
    severity: severity,
    action: action,
    priority: priority
  };
}

function runWorkload(count) {
  var logs = [];

  for (var i = 0; i < count; i++) {
    logs.push({
      service: serviceAt(i),
      message: messageAt(i),
      level: levelAt(i)
    });
  }

  var severityCounts = {};
  var actionCounts = {};
  var critical = 0;
  var pageable = 0;

  for (var j = 0; j < logs.length; j++) {
    var result = classify(logs[j]);
    severityCounts[result.severity] = (severityCounts[result.severity] || 0) + 1;
    actionCounts[result.action] = (actionCounts[result.action] || 0) + 1;

    if (result.severity === "critical") {
      critical++;
    }

    if (result.action === "page") {
      pageable++;
    }
  }

  var warnings = severityCounts["warning"] || 0;
  return "logs=" + logs.length
      + " critical=" + critical
      + " warnings=" + warnings
      + " page=" + pageable
      + " severities=" + Object.keys(severityCounts).length
      + " actions=" + Object.keys(actionCounts).length;
}
