package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserTypeErrorTest {
  @Test
  void reportsMissingParameterType() {
    ParseException error =
        assertThrows(
            ParseException.class, () -> parse("function add(a): number { return a; };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("type annotation"));
    assertTrue(error.getMessage().contains("a"));
  }

  @Test
  void reportsMissingReturnType() {
    ParseException error =
        assertThrows(
            ParseException.class, () -> parse("function add(a: number) { return a; };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("return type"));
  }

  @Test
  void reportsMissingTypeName() {
    ParseException error =
        assertThrows(
            ParseException.class, () -> parse("function add(a: ): number { return a; };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("type name"));
  }

}
