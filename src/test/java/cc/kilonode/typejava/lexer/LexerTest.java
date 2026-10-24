package cc.kilonode.typejava.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LexerTest {
  @Test
  void tokenizesConsoleLogCall() {
    List<Token> tokens = new Lexer("console.log(\"hello world\");").tokenize();

    assertEquals(
        List.of(
            TokenType.IDENTIFIER,
            TokenType.DOT,
            TokenType.IDENTIFIER,
            TokenType.LPAREN,
            TokenType.STRING,
            TokenType.RPAREN,
            TokenType.SEMICOLON,
            TokenType.EOF),
        tokens.stream().map(Token::type).toList());
    assertEquals("console", tokens.get(0).lexeme());
    assertEquals("log", tokens.get(2).lexeme());
    assertEquals("hello world", tokens.get(4).value());
  }

  @Test
  void tokenizesPunctuation() {
    List<Token> tokens = new Lexer(".,();").tokenize();

    assertEquals(
        List.of(
            TokenType.DOT,
            TokenType.COMMA,
            TokenType.LPAREN,
            TokenType.RPAREN,
            TokenType.SEMICOLON,
            TokenType.EOF),
        tokens.stream().map(Token::type).toList());
  }

  @Test
  void returnsOnlyEofForEmptyInput() {
    List<Token> tokens = new Lexer("").tokenize();

    assertEquals(List.of(TokenType.EOF), tokens.stream().map(Token::type).toList());
    assertEquals(1, tokens.get(0).line());
  }
}
