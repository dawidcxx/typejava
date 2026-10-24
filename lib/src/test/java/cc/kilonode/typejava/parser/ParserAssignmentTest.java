package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.AssignmentExpression;
import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.Program;
import org.junit.jupiter.api.Test;

class ParserAssignmentTest {
  @Test
  void parsesFieldAssignment() {
    Program program = parse("point.x = point.x + 1;");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    assertEquals(
        new AssignmentExpression(
            new MemberExpression(new Identifier("point"), "x"),
            new BinaryExpression(
                new MemberExpression(new Identifier("point"), "x"),
                "+",
                new NumericLiteral("1"))),
        statement.expression());
  }

  @Test
  void parsesNestedFieldAssignment() {
    Program program = parse("point.inner.x = 2;");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    assertEquals(
        new AssignmentExpression(
            new MemberExpression(
                new MemberExpression(new Identifier("point"), "inner"), "x"),
            new NumericLiteral("2")),
        statement.expression());
  }
}
