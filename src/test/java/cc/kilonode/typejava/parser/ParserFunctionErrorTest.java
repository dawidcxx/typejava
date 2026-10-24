package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserFunctionErrorTest {
  @Test
  void reportsMissingFunctionName() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("function (): number {};"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("function name"));
  }

  @Test
  void reportsMissingSemicolonAfterDeclaration() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("function ping(): number { return 1; }"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("semicolon"));
  }

  @Test
  void reportsMissingClosingBrace() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("function ping(): number { return 1;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected '}'"));
  }

  @Test
  void reportsMissingReturnExpression() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("function ping(): number { return; };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected expression"));
  }

  @Test
  void reportsMissingParameterName() {
    ParseException error =
        assertThrows(
            ParseException.class, () -> parse("function add(: number): number { return 1; };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("parameter name"));
  }
}
