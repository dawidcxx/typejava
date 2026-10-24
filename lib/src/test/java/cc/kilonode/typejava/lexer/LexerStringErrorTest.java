package cc.kilonode.typejava.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LexerStringErrorTest {
  @Test
  void reportsUnterminatedStringAtEndOfInput() {
    LexerException error =
        assertThrows(LexerException.class, () -> new Lexer("console.log(\"hello);").tokenize());

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Unterminated string"));
  }

  @Test
  void reportsUnterminatedStringThatReachesNewline() {
    LexerException error =
        assertThrows(
            LexerException.class, () -> new Lexer("console.log(\"hello\nworld\");").tokenize());

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Unterminated string"));
  }

  @Test
  void rejectsInvalidEscapeSequence() {
    LexerException error =
        assertThrows(LexerException.class, () -> new Lexer("\"\\q\"").tokenize());

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Invalid escape"));
  }

  @Test
  void rejectsInvalidUnicodeEscape() {
    LexerException error =
        assertThrows(LexerException.class, () -> new Lexer("\"\\u12xz\"").tokenize());

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("unicode"));
  }
}
