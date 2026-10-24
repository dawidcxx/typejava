package cc.kilonode.typejava.lexer;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {
  private final String source;
  private int index;
  private int line = 1;
  private int column = 1;

  public Lexer(String source) {
    this.source = source;
  }

  public List<Token> tokenize() {
    List<Token> tokens = new ArrayList<>();

    while (!isAtEnd()) {
      skipWhitespace();
      if (isAtEnd()) {
        break;
      }

      int tokenLine = line;
      int tokenColumn = column;
      int start = index;
      char c = peek();

      if (isIdentifierStart(c)) {
        advance();
        while (isIdentifierPart(peek())) {
          advance();
        }
        tokens.add(new Token(TokenType.IDENTIFIER, source.substring(start, index), null, tokenLine, tokenColumn));
        continue;
      }

      switch (c) {
        case '"' -> tokens.add(scanString(tokenLine, tokenColumn));
        case '.' -> {
          advance();
          tokens.add(new Token(TokenType.DOT, ".", null, tokenLine, tokenColumn));
        }
        case '(' -> {
          advance();
          tokens.add(new Token(TokenType.LPAREN, "(", null, tokenLine, tokenColumn));
        }
        case ')' -> {
          advance();
          tokens.add(new Token(TokenType.RPAREN, ")", null, tokenLine, tokenColumn));
        }
        case ';' -> {
          advance();
          tokens.add(new Token(TokenType.SEMICOLON, ";", null, tokenLine, tokenColumn));
        }
        case ',' -> {
          advance();
          tokens.add(new Token(TokenType.COMMA, ",", null, tokenLine, tokenColumn));
        }
        case '\'' ->
            throw new LexerException(
                "Single-quoted strings are not supported; use double quotes", tokenLine, tokenColumn);
        case '`' ->
            throw new LexerException(
                "Backtick strings are not supported; use double quotes", tokenLine, tokenColumn);
        default ->
            throw new LexerException("Unexpected character '" + c + "'", tokenLine, tokenColumn);
      }
    }

    tokens.add(new Token(TokenType.EOF, "", null, line, column));
    return tokens;
  }

  private Token scanString(int tokenLine, int tokenColumn) {
    int start = index;
    advance();

    StringBuilder value = new StringBuilder();
    while (!isAtEnd()) {
      char c = peek();

      if (c == '"') {
        advance();
        return new Token(TokenType.STRING, source.substring(start, index), value.toString(), tokenLine, tokenColumn);
      }

      if (c == '\n' || c == '\r') {
        throw new LexerException("Unterminated string literal. Strings cannot span new lines", tokenLine, tokenColumn);
      }

      if (c == '\\') {
        advance();
        if (isAtEnd()) {
          throw new LexerException("Unterminated string literal", tokenLine, tokenColumn);
        }

        char escaped = advance();
        switch (escaped) {
          case '"' -> value.append('"');
          case '\\' -> value.append('\\');
          case '/' -> value.append('/');
          case 'b' -> value.append('\b');
          case 'f' -> value.append('\f');
          case 'n' -> value.append('\n');
          case 'r' -> value.append('\r');
          case 't' -> value.append('\t');
          case 'u' -> value.append(scanUnicodeEscape());
          default -> throw new LexerException("Invalid escape sequence '\\" + escaped + "'", line, column);
        }
        continue;
      }

      value.append(advance());
    }

    throw new LexerException("Unterminated string literal", tokenLine, tokenColumn);
  }

  private char scanUnicodeEscape() {
    int value = 0;
    for (int i = 0; i < 4; i++) {
      if (isAtEnd()) {
        throw new LexerException("Invalid unicode escape sequence", line, column);
      }
      int digit = Character.digit(peek(), 16);
      if (digit < 0) {
        throw new LexerException("Invalid unicode escape sequence", line, column);
      }
      value = value * 16 + digit;
      advance();
    }
    return (char) value;
  }

  private void skipWhitespace() {
    while (!isAtEnd()) {
      char c = peek();
      if (c == ' ' || c == '\t' || c == '\f' || c == '\n' || c == '\r') {
        advance();
      } else {
        break;
      }
    }
  }

  private char advance() {
    char c = source.charAt(index);

    if (c == '\r') {
      index++;
      if (index < source.length() && source.charAt(index) == '\n') {
        index++;
      }
      line++;
      column = 1;
    } else {
      index++;
      if (c == '\n') {
        line++;
        column = 1;
      } else {
        column++;
      }
    }

    return c;
  }

  private char peek() {
    return isAtEnd() ? '\0' : source.charAt(index);
  }

  private boolean isAtEnd() {
    return index >= source.length();
  }

  private static boolean isIdentifierStart(char c) {
    return c == '_' || c == '$' || Character.isLetter(c);
  }

  private static boolean isIdentifierPart(char c) {
    return isIdentifierStart(c) || Character.isDigit(c);
  }
}
