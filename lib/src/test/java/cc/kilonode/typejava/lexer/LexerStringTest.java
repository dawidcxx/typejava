package cc.kilonode.typejava.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LexerStringTest {
  @Test
  void decodesStringEscapes() {
    List<Token> tokens = new Lexer("\"a\\n\\\"b\"").tokenize();

    assertEquals("a\n\"b", tokens.get(0).value());
  }

  @Test
  void decodesBackslashEscapes() {
    List<Token> tokens = new Lexer("\"a\\\\b\\tc\"").tokenize();

    assertEquals("a\\b\tc", tokens.get(0).value());
  }

  @Test
  void acceptsEmptyString() {
    List<Token> tokens = new Lexer("\"\"").tokenize();

    assertEquals(TokenType.STRING, tokens.get(0).type());
    assertEquals("", tokens.get(0).value());
  }

  @Test
  void keepsTheRawLexeme() {
    List<Token> tokens = new Lexer("\"a\\nb\"").tokenize();

    assertEquals("\"a\\nb\"", tokens.get(0).lexeme());
  }
}
