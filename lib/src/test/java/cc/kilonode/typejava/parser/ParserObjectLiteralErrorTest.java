package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserObjectLiteralErrorTest {
  @Test
  void reportsMissingFieldName() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("let point: Point = { : 1 };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("field name"));
  }

  @Test
  void reportsMissingFieldColon() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("let point: Point = { x 1 };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected ':'"));
  }

  @Test
  void reportsMissingCommaBetweenFields() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("let point: Point = { x: 1 y: 2 };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected '}'"));
  }

  @Test
  void reportsMissingClosingBrace() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("let point: Point = { x: 1;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected '}'"));
  }
}
