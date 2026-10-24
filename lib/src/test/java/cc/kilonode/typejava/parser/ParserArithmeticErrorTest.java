package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserArithmeticErrorTest {
  @Test
  void rejectsConsecutiveUnaryMinus() {
    ParseException error = assertThrows(ParseException.class, () -> parse("let x: number = - -a;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Consecutive '-'"));
    assertTrue(error.getMessage().contains("parentheses"));
  }

  @Test
  void rejectsUnaryMinusDirectlyAfterBinaryMinus() {
    ParseException error = assertThrows(ParseException.class, () -> parse("let x: number = a - -b;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Consecutive '-'"));
    assertTrue(error.getMessage().contains("parentheses"));
  }
}
