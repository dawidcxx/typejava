package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.AssignmentExpression;
import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.BooleanLiteral;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.UnaryExpression;
import org.junit.jupiter.api.Test;

class ParserOperatorExpressionTest {
  @Test
  void parsesAssignment() {
    Program program = parse("value = 1 + 2;");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    assertEquals(
        new AssignmentExpression(
            new Identifier("value"),
            new BinaryExpression(new NumericLiteral("1"), "+", new NumericLiteral("2"))),
        statement.expression());
  }

  @Test
  void parsesBooleanLiterals() {
    Program program = parse("true; false;");

    assertEquals(
        new ExpressionStatement(new BooleanLiteral(true)), program.statements().get(0));
    assertEquals(
        new ExpressionStatement(new BooleanLiteral(false)), program.statements().get(1));
  }

  @Test
  void parsesComparison() {
    Program program = parse("a < b;");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    assertEquals(
        new BinaryExpression(new Identifier("a"), "<", new Identifier("b")),
        statement.expression());
  }

  @Test
  void appliesLogicalPrecedence() {
    Program program = parse("a || b && c;");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    assertEquals(
        new BinaryExpression(
            new Identifier("a"),
            "||",
            new BinaryExpression(new Identifier("b"), "&&", new Identifier("c"))),
        statement.expression());
  }

  @Test
  void parsesUnaryNot() {
    Program program = parse("!done;");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    assertEquals(new UnaryExpression("!", new Identifier("done")), statement.expression());
  }
}
