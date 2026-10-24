package cc.kilonode.typejava.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LexerCommentTest {
  @Test
  void skipsLineComments() {
    List<Token> tokens = new Lexer("// ignored\n123").tokenize();

    assertEquals(TokenType.NUMBER, tokens.get(0).type());
    assertEquals(2, tokens.get(0).line());
  }

  @Test
  void skipsBlockComments() {
    List<Token> tokens = new Lexer("/* ignored\nstill ignored */ 123").tokenize();

    assertEquals(TokenType.NUMBER, tokens.get(0).type());
    assertEquals(2, tokens.get(0).line());
    assertEquals(18, tokens.get(0).column());
  }

  @Test
  void skipsCommentsBetweenTokens() {
    List<Token> tokens = new Lexer("1 /* middle */ + // trailing\n2").tokenize();

    assertEquals(
        List.of(TokenType.NUMBER, TokenType.PLUS, TokenType.NUMBER, TokenType.EOF),
        tokens.stream().map(Token::type).toList());
  }

  @Test
  void reportsUnterminatedBlockComment() {
    LexerException error =
        assertThrows(LexerException.class, () -> new Lexer("/* never closed").tokenize());

    assertEquals(1, error.getLine());
    assertEquals(1, error.getColumn());
    assertTrue(error.getMessage().contains("Unterminated block comment"));
  }
}
