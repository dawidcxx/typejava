package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserUnionErrorTest {
  @Test
  void reportsMissingUnionMemberAfterPipe() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("type Broken = A |;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("type name"));
  }

  @Test
  void reportsMissingUnionMemberAtStart() {
    ParseException error =
        assertThrows(
            ParseException.class, () -> parse("function f(value: | A): void {};"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("type name"));
  }
}
