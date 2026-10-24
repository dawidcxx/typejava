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
        String text = source.substring(start, index);
        TokenType type =
            switch (text) {
              case "function" -> TokenType.FUNCTION;
              case "return" -> TokenType.RETURN;
              case "type" -> TokenType.TYPE;
              case "let" -> TokenType.LET;
              case "const" -> TokenType.CONST;
              case "readonly" -> TokenType.READONLY;
              case "if" -> TokenType.IF;
              case "else" -> TokenType.ELSE;
              case "while" -> TokenType.WHILE;
              case "break" -> TokenType.BREAK;
              case "continue" -> TokenType.CONTINUE;
              case "true" -> TokenType.TRUE;
              case "false" -> TokenType.FALSE;
              default -> TokenType.IDENTIFIER;
            };
        tokens.add(new Token(type, text, null, tokenLine, tokenColumn));
        continue;
      }

      if (Character.isDigit(c)) {
        tokens.add(scanNumber(tokenLine, tokenColumn));
        continue;
      }

      switch (c) {
        case '"' -> tokens.add(scanString(tokenLine, tokenColumn));
        case '.' -> {
          advance();
          tokens.add(new Token(TokenType.DOT, ".", null, tokenLine, tokenColumn));
        }
        case ':' -> {
          advance();
          tokens.add(new Token(TokenType.COLON, ":", null, tokenLine, tokenColumn));
        }
        case '{' -> {
          advance();
          tokens.add(new Token(TokenType.LBRACE, "{", null, tokenLine, tokenColumn));
        }
        case '}' -> {
          advance();
          tokens.add(new Token(TokenType.RBRACE, "}", null, tokenLine, tokenColumn));
        }
        case '+' -> {
          advance();
          tokens.add(new Token(TokenType.PLUS, "+", null, tokenLine, tokenColumn));
        }
        case '-' -> {
          advance();
          tokens.add(new Token(TokenType.MINUS, "-", null, tokenLine, tokenColumn));
        }
        case '*' -> {
          advance();
          tokens.add(new Token(TokenType.STAR, "*", null, tokenLine, tokenColumn));
        }
        case '/' -> {
          advance();
          tokens.add(new Token(TokenType.SLASH, "/", null, tokenLine, tokenColumn));
        }
        case '%' -> {
          advance();
          tokens.add(new Token(TokenType.PERCENT, "%", null, tokenLine, tokenColumn));
        }
        case '=' -> {
          advance();
          TokenType type = match('=') ? TokenType.EQEQ : TokenType.ASSIGN;
          tokens.add(new Token(type, source.substring(start, index), null, tokenLine, tokenColumn));
        }
        case '!' -> {
          advance();
          TokenType type = match('=') ? TokenType.BANGEQ : TokenType.BANG;
          tokens.add(new Token(type, source.substring(start, index), null, tokenLine, tokenColumn));
        }
        case '<' -> {
          advance();
          TokenType type = match('=') ? TokenType.LTE : TokenType.LT;
          tokens.add(new Token(type, source.substring(start, index), null, tokenLine, tokenColumn));
        }
        case '>' -> {
          advance();
          TokenType type = match('=') ? TokenType.GTE : TokenType.GT;
          tokens.add(new Token(type, source.substring(start, index), null, tokenLine, tokenColumn));
        }
        case '&' -> {
          advance();
          if (!match('&')) {
            throw new LexerException("Unexpected character '&'", tokenLine, tokenColumn);
          }
          tokens.add(new Token(TokenType.ANDAND, "&&", null, tokenLine, tokenColumn));
        }
        case '|' -> {
          advance();
          if (!match('|')) {
            throw new LexerException("Unexpected character '|'", tokenLine, tokenColumn);
          }
          tokens.add(new Token(TokenType.OROR, "||", null, tokenLine, tokenColumn));
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

  private Token scanNumber(int tokenLine, int tokenColumn) {
    int start = index;

    while (Character.isDigit(peek())) {
      advance();
    }

    if (peek() == '.' && Character.isDigit(peekNext())) {
      advance();
      while (Character.isDigit(peek())) {
        advance();
      }
    }

    return new Token(TokenType.NUMBER, source.substring(start, index), null, tokenLine, tokenColumn);
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
      } else if (c == '/' && peekNext() == '/') {
        skipLineComment();
      } else if (c == '/' && peekNext() == '*') {
        skipBlockComment();
      } else {
        break;
      }
    }
  }

  private void skipLineComment() {
    advance();
    advance();

    while (!isAtEnd() && peek() != '\n' && peek() != '\r') {
      advance();
    }
  }

  private void skipBlockComment() {
    int startLine = line;
    int startColumn = column;
    advance();
    advance();

    while (!isAtEnd()) {
      if (peek() == '*' && peekNext() == '/') {
        advance();
        advance();
        return;
      }
      advance();
    }

    throw new LexerException("Unterminated block comment", startLine, startColumn);
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

  private char peekNext() {
    return index + 1 >= source.length() ? '\0' : source.charAt(index + 1);
  }

  private boolean match(char expected) {
    if (peek() != expected) {
      return false;
    }
    advance();
    return true;
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
