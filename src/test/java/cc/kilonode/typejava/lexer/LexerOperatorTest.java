package cc.kilonode.typejava.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LexerOperatorTest {
  @Test
  void tokenizesArithmeticOperators() {
    List<Token> tokens = new Lexer("+ - * / %").tokenize();

    assertEquals(
        List.of(
            TokenType.PLUS,
            TokenType.MINUS,
            TokenType.STAR,
            TokenType.SLASH,
            TokenType.PERCENT,
            TokenType.EOF),
        tokens.stream().map(Token::type).toList());
  }

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

  @Test
  void tokenizesUnionPipe() {
    List<Token> tokens = new Lexer("A | B").tokenize();

    assertEquals(
        List.of(
            TokenType.IDENTIFIER,
            TokenType.PIPE,
            TokenType.IDENTIFIER,
            TokenType.EOF),
        tokens.stream().map(Token::type).toList());
  }

  @Test
  void tokenizesNestedGenericBrackets() {
    List<Token> tokens = new Lexer("Box<TjArray<number>>").tokenize();

    assertEquals(
        List.of(
            TokenType.IDENTIFIER,
            TokenType.LT,
            TokenType.IDENTIFIER,
            TokenType.LT,
            TokenType.IDENTIFIER,
            TokenType.GT,
            TokenType.GT,
            TokenType.EOF),
        tokens.stream().map(Token::type).toList());
  }
}
