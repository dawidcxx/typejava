package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.BooleanLiteral;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.TypeReference;
import cc.kilonode.typejava.ast.VariableDeclaration;
import cc.kilonode.typejava.ast.VariableKind;
import org.junit.jupiter.api.Test;

class ParserVariableTest {
  @Test
  void parsesLetDeclaration() {
    Program program = parse("let total: number = 1 + 2;");

    VariableDeclaration declaration = (VariableDeclaration) program.statements().get(0);
    assertEquals(VariableKind.LET, declaration.kind());
    assertEquals("total", declaration.name());
    assertEquals(new TypeReference("number"), declaration.type());
    assertEquals(
        new BinaryExpression(new NumericLiteral("1"), "+", new NumericLiteral("2")),
        declaration.initializer());
  }

  @Test
  void parsesConstDeclaration() {
    Program program = parse("const enabled: boolean = true;");

    VariableDeclaration declaration = (VariableDeclaration) program.statements().get(0);
    assertEquals(VariableKind.CONST, declaration.kind());
    assertEquals("enabled", declaration.name());
    assertEquals(new TypeReference("boolean"), declaration.type());
    assertEquals(new BooleanLiteral(true), declaration.initializer());
  }
}
