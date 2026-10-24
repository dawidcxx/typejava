package cc.kilonode.typejava.lexer;

import java.util.List;

final class TokenSnapshot {
  private TokenSnapshot() {}

  static String render(List<Token> tokens) {
    StringBuilder snapshot = new StringBuilder();

    for (Token token : tokens) {
      snapshot
          .append(pad(token.type().name(), 12))
          .append(' ')
          .append(pad(quote(token.lexeme()), 24))
          .append(" @ ")
          .append(token.line())
          .append(':')
          .append(token.column());

      if (token.value() != null) {
        snapshot.append(" value=").append(quote(token.value()));
      }

      snapshot.append('\n');
    }

    return snapshot.toString();
  }

  private static String quote(String value) {
    StringBuilder quoted = new StringBuilder("\"");

    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '\\' -> quoted.append("\\\\");
        case '"' -> quoted.append("\\\"");
        case '\n' -> quoted.append("\\n");
        case '\r' -> quoted.append("\\r");
        case '\t' -> quoted.append("\\t");
        default -> quoted.append(c);
      }
    }

    return quoted.append('"').toString();
  }

  private static String pad(String value, int width) {
    return value.length() >= width ? value : value + " ".repeat(width - value.length());
  }
}
