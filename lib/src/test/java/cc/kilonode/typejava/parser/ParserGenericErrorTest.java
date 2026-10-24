package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserGenericErrorTest {
  @Test
  void reportsMissingTypeParameterName() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("type Box<> = {};"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("type parameter name"));
  }

  @Test
  void reportsMissingTypeParameterClose() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("type Box<T = {};"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("after type parameters"));
  }

  @Test
  void reportsMissingTypeArgument() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("let boxed: Box<> = null;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected a type argument"));
  }

  @Test
  void reportsMissingTypeArgumentClose() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("let boxed: Box<number = null;"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("after type arguments"));
  }
}
