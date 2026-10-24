package cc.kilonode.typejava.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LexerOperatorTest {
  @Test
  void tokenizesAssignmentAndEqualityOperators() {
    List<Token> tokens = new Lexer("= == != < <= > >=").tokenize();

    assertEquals(
        List.of(
            TokenType.ASSIGN,
            TokenType.EQEQ,
            TokenType.BANGEQ,
            TokenType.LT,
            TokenType.LTE,
            TokenType.GT,
            TokenType.GTE,
            TokenType.EOF),
        tokens.stream().map(Token::type).toList());
  }

  @Test
  void tokenizesLogicalOperators() {
    List<Token> tokens = new Lexer("&& || !").tokenize();

    assertEquals(
        List.of(
            TokenType.ANDAND,
            TokenType.OROR,
            TokenType.BANG,
            TokenType.EOF),
        tokens.stream().map(Token::type).toList());
  }
}
