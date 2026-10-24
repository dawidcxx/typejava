package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserVariableErrorTest {
  @Test
  void reportsMissingVariableType() {
    ParseException error = assertThrows(ParseException.class, () -> parse("let total = 1;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("type annotation"));
    assertTrue(error.getMessage().contains("total"));
  }

  @Test
  void reportsMissingInitializer() {
    ParseException error = assertThrows(ParseException.class, () -> parse("let total: number;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("initializer"));
  }

  @Test
  void reportsMissingSemicolonAfterDeclaration() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("let total: number = 1"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("semicolon"));
  }
}
