package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserJumpErrorTest {
  @Test
  void reportsMissingSemicolonAfterBreak() {
    ParseException error = assertThrows(ParseException.class, () -> parse("break"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("after break"));
  }

  @Test
  void reportsMissingSemicolonAfterContinue() {
    ParseException error = assertThrows(ParseException.class, () -> parse("continue"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("after continue"));
  }
}
