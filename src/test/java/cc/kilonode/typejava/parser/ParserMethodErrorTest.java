package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserMethodErrorTest {
  @Test
  void reportsMissingMethodReturnType() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("type X = { f(this: X): ; };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("type name"));
  }

  @Test
  void reportsMissingMethodBody() {
    ParseException error =
        assertThrows(
            ParseException.class, () -> parse("let x: X = { f(): number };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected '{'"));
  }

  @Test
  void rejectsReadonlyMethod() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("type X = { readonly f(): void; };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Methods cannot be readonly"));
  }
}
