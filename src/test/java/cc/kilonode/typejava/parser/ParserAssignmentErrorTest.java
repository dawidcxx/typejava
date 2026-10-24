package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserAssignmentErrorTest {
  @Test
  void rejectsLiteralAssignmentTarget() {
    ParseException error = assertThrows(ParseException.class, () -> parse("1 = 2;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Invalid assignment target"));
  }

  @Test
  void rejectsExpressionAssignmentTarget() {
    ParseException error = assertThrows(ParseException.class, () -> parse("a + b = 1;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Invalid assignment target"));
  }

  @Test
  void rejectsCallAssignmentTarget() {
    ParseException error = assertThrows(ParseException.class, () -> parse("f() = 1;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Invalid assignment target"));
  }
}
