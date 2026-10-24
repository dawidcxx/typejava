package cc.kilonode.typejava.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LexerKeywordTest {
  @Test
  void recognizesFunctionAndReturnKeywords() {
    List<Token> tokens = new Lexer("function return").tokenize();

    assertEquals(
        List.of(TokenType.FUNCTION, TokenType.RETURN, TokenType.EOF),
        tokens.stream().map(Token::type).toList());
  }

  @Test
  void treatsKeywordsAsWholeWords() {
    List<Token> tokens = new Lexer("functionality returnValue").tokenize();

    assertEquals(
        List.of(TokenType.IDENTIFIER, TokenType.IDENTIFIER, TokenType.EOF),
        tokens.stream().map(Token::type).toList());
  }
}
