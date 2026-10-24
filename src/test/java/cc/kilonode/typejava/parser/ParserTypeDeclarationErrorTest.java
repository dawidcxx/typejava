package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParserTypeDeclarationErrorTest {
  @Test
  void reportsMissingTypeName() {
    ParseException error = assertThrows(ParseException.class, () -> parse("type = {};"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("type name"));
  }

  @Test
  void reportsMissingEquals() {
    ParseException error = assertThrows(ParseException.class, () -> parse("type Foo {};"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("Expected '='"));
  }

  @Test
  void reportsMissingFieldName() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("type Foo = { : number; };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("field name"));
  }

  @Test
  void reportsMissingFieldType() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("type Foo = { a: ; };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("type name or object type"));
  }

  @Test
  void reportsMissingFieldSemicolon() {
    ParseException error =
        assertThrows(ParseException.class, () -> parse("type Foo = { a: number };"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("after field 'a'"));
  }

  @Test
  void reportsMissingDeclarationSemicolon() {
    ParseException error = assertThrows(ParseException.class, () -> parse("type Foo = {}"));

    assertEquals(1, error.getLine());
    assertTrue(error.getMessage().contains("type declaration"));
  }
}
