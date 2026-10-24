package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserLoopErrorTest {
  @Test
  void reportsMissingOpeningParenthesis() {
    ParseException error = assertThrows(ParseException.class, () -> parse("while true {}"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected '('"));
  }

  @Test
  void reportsMissingClosingParenthesis() {
    ParseException error = assertThrows(ParseException.class, () -> parse("while (true {}"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected ')'"));
  }

  @Test
  void reportsMissingLoopBody() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("while (true) return;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected '{'"));
  }
}
