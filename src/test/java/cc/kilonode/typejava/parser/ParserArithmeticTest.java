package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.UnaryExpression;
import org.junit.jupiter.api.Test;

class ParserArithmeticTest {
  @Test
  void appliesMultiplicationBeforeAddition() {
    Program program = parse("a + b * c;");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    assertEquals(
        new BinaryExpression(
            new Identifier("a"),
            "+",
            new BinaryExpression(new Identifier("b"), "*", new Identifier("c"))),
        statement.expression());
  }

  @Test
  void appliesArithmeticLeftToRight() {
    Program program = parse("a - b - c;");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    assertEquals(
        new BinaryExpression(
            new BinaryExpression(new Identifier("a"), "-", new Identifier("b")),
            "-",
            new Identifier("c")),
        statement.expression());
  }

  @Test
  void parsesDivisionAndRemainder() {
    Program program = parse("a / b % c;");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    assertEquals(
        new BinaryExpression(
            new BinaryExpression(new Identifier("a"), "/", new Identifier("b")),
            "%",
            new Identifier("c")),
        statement.expression());
  }

  @Test
  void parsesUnaryMinus() {
    Program program = parse("-a + b;");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    assertEquals(
        new BinaryExpression(
            new UnaryExpression("-", new Identifier("a")), "+", new Identifier("b")),
        statement.expression());
  }

  @Test
  void parsesParenthesizedNegationAfterSubtraction() {
    Program program = parse("a - (-b);");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    assertEquals(
        new BinaryExpression(
            new Identifier("a"), "-", new UnaryExpression("-", new Identifier("b"))),
        statement.expression());
  }
}
