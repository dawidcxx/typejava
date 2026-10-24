package cc.kilonode.typejava.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LexerLineTrackingTest {
  @Test
  void tracksLinesAcrossUnixAndWindowsNewlines() {
    String source = "console.log(\"a\");\r\nconsole.log(\"b\");\n\tconsole.log(\"c\");";

    List<Token> tokens = new Lexer(source).tokenize();

    List<Token> identifiers =
        tokens.stream().filter(t -> t.type() == TokenType.IDENTIFIER).toList();
    assertEquals(
        List.of("console", "log", "console", "log", "console", "log"),
        identifiers.stream().map(Token::lexeme).toList());
    assertEquals(1, identifiers.get(0).line());
    assertEquals(2, identifiers.get(2).line());
    assertEquals(3, identifiers.get(4).line());
  }

  @Test
  void tracksColumnsFromTheStartOfEachLine() {
    List<Token> tokens = new Lexer("  console\r\n\tlog").tokenize();

    assertEquals(3, tokens.get(0).column());
    assertEquals(2, tokens.get(1).column());
  }
}
