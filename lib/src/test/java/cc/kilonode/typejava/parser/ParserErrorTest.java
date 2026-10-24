package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserErrorTest {
  @Test
  void reportsMissingSemicolonWithLine() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("console.log(\"hello world\")"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("semicolon"));
  }

  @Test
  void reportsMissingSemicolonOnTheCorrectLine() {
    ParseException error =
        assertThrows(
            ParseException.class,
            () -> parse("console.log(\"ok\");\r\nconsole.log(\"hello world\")"));

    assertEquals(2, error.getLine());
    assertTrue(error.getMessage().contains("semicolon"));
  }

  @Test
  void reportsMissingExpression() {
    ParseException error = assertThrows(ParseException.class, () -> parse("console.log(;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected expression"));
  }

  @Test
  void reportsMissingClosingParenthesis() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("console.log(\"hello\";"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected ')'"));
  }

  @Test
  void reportsUnexpectedTrailingToken() {
    ParseException error =
        assertThrows(
            ParseException.class, () -> parse("console.log(\"a\") console.log(\"b\");"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("semicolon"));
  }

  @Test
  void reportsMissingPropertyName() {
    ParseException error = assertThrows(ParseException.class, () -> parse("console.;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("property name"));
  }
}
