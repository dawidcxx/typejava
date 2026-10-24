package cc.kilonode.typejava.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LexerErrorTest {
  @Test
  void rejectsSingleQuotedStringsWithHelpfulMessage() {
    LexerException error =
        assertThrows(LexerException.class, () -> new Lexer("console.log('hello');").tokenize());

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("double quotes"));
  }

  @Test
  void rejectsBacktickStringsWithHelpfulMessage() {
    LexerException error =
        assertThrows(LexerException.class, () -> new Lexer("console.log(`hello`);").tokenize());

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("double quotes"));
  }

  @Test
  void rejectsUnexpectedCharacter() {
    LexerException error =
        assertThrows(LexerException.class, () -> new Lexer("console.log(@);").tokenize());

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("@"));
  }

  @Test
  void reportsErrorsOnTheCorrectLine() {
    LexerException error =
        assertThrows(
            LexerException.class,
            () -> new Lexer("console.log(\"ok\");\r\nconsole.log('no');").tokenize());

    assertEquals(2, error.getLine());
  }

  @Test
  void rejectsSingleAmpersand() {
    LexerException error =
        assertThrows(LexerException.class, () -> new Lexer("true & false").tokenize());

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("&"));
  }
}
