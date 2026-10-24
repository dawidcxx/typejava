package cc.kilonode.typejava.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LexerKeywordTest {
  @Test
  void recognizesKeywords() {
    List<Token> tokens = new Lexer("function return type let const if else true false").tokenize();

    assertEquals(
        List.of(
            TokenType.FUNCTION,
            TokenType.RETURN,
            TokenType.TYPE,
            TokenType.LET,
            TokenType.CONST,
            TokenType.IF,
            TokenType.ELSE,
            TokenType.TRUE,
            TokenType.FALSE,
            TokenType.EOF),
        tokens.stream().map(Token::type).toList());
  }

  @Test
  void treatsKeywordsAsWholeWords() {
    List<Token> tokens = new Lexer("functionality returnValue letter constant").tokenize();

    assertEquals(
        List.of(
            TokenType.IDENTIFIER,
            TokenType.IDENTIFIER,
            TokenType.IDENTIFIER,
            TokenType.IDENTIFIER,
            TokenType.EOF),
        tokens.stream().map(Token::type).toList());
  }
}
