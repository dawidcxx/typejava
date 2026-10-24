package cc.kilonode.typejava.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LexerNumberTest {
  @Test
  void tokenizesIntegerLiteral() {
    List<Token> tokens = new Lexer("123").tokenize();

    assertEquals(TokenType.NUMBER, tokens.get(0).type());
    assertEquals("123", tokens.get(0).lexeme());
  }

  @Test
  void tokenizesDecimalLiteral() {
    List<Token> tokens = new Lexer("12.5").tokenize();

    assertEquals(TokenType.NUMBER, tokens.get(0).type());
    assertEquals("12.5", tokens.get(0).lexeme());
  }

  @Test
  void keepsDotSeparateFromMemberAccess() {
    List<Token> tokens = new Lexer("1.toString").tokenize();

    assertEquals(
        List.of(TokenType.NUMBER, TokenType.DOT, TokenType.IDENTIFIER, TokenType.EOF),
        tokens.stream().map(Token::type).toList());
  }
}
