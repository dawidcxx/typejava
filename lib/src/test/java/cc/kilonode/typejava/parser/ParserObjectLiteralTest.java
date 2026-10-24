package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.BooleanLiteral;
import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.Expression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.ObjectField;
import cc.kilonode.typejava.ast.ObjectLiteral;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.VariableDeclaration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserObjectLiteralTest {
  @Test
  void parsesObjectLiteralWithFields() {
    Program program = parse("let point: Point = { x: 1, y: 2 };");

    VariableDeclaration declaration = (VariableDeclaration) program.statements().get(0);
    assertEquals(
        new ObjectLiteral(
            List.of(
                new ObjectField("x", new NumericLiteral("1")),
                new ObjectField("y", new NumericLiteral("2")))),
        declaration.initializer());
  }

  @Test
  void parsesEmptyObjectLiteral() {
    Program program = parse("let value: Empty = {};");

    VariableDeclaration declaration = (VariableDeclaration) program.statements().get(0);
    assertEquals(new ObjectLiteral(List.of()), declaration.initializer());
  }

  @Test
  void parsesNestedObjectLiteral() {
    Program program = parse("let point: Point = { x: 1, inner: { enabled: true } };");

    VariableDeclaration declaration = (VariableDeclaration) program.statements().get(0);
    ObjectLiteral outer = (ObjectLiteral) declaration.initializer();
    assertEquals(
        new ObjectLiteral(List.of(new ObjectField("enabled", new BooleanLiteral(true)))),
        outer.fields().get(1).value());
  }

  @Test
  void parsesObjectLiteralAsCallArgument() {
    Program program = parse("console.log({ x: 1 });");

    CallExpression call =
        (CallExpression) ((ExpressionStatement) program.statements().get(0)).expression();
    assertEquals(
        List.of(new ObjectLiteral(List.of(new ObjectField("x", new NumericLiteral("1"))))),
        call.arguments());
  }

  @Test
  void allowsTrailingComma() {
    Program program = parse("let point: Point = { x: 1, };");

    VariableDeclaration declaration = (VariableDeclaration) program.statements().get(0);
    assertEquals(
        new ObjectLiteral(List.of(new ObjectField("x", new NumericLiteral("1")))),
        declaration.initializer());
  }

  @Test
  void parsesMemberAccessOnObjectLiteral() {
    Program program = parse("({ x: 1 }).x;");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    Expression expression = statement.expression();
    assertEquals(
        new MemberExpression(
            new ObjectLiteral(List.of(new ObjectField("x", new NumericLiteral("1")))),
            "x"),
        expression);
  }
}
