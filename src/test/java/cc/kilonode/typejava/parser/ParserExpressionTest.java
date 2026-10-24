package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.StringLiteral;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserExpressionTest {
  @Test
  void parsesMultipleArguments() {
    CallExpression call = firstCall(parse("console.log(\"a\", \"b\");"));

    assertEquals(List.of(new StringLiteral("a"), new StringLiteral("b")), call.arguments());
  }

  @Test
  void parsesEmptyArgumentList() {
    CallExpression call = firstCall(parse("console.log();"));

    assertEquals(List.of(), call.arguments());
  }

  @Test
  void parsesChainedMemberAccess() {
    Program program = parse("console.log.name;");

    assertEquals(
        new Program(
            List.of(
                new ExpressionStatement(
                    new MemberExpression(
                        new MemberExpression(new Identifier("console"), "log"), "name")))),
        program);
  }

  @Test
  void parsesNestedCalls() {
    CallExpression outer = firstCall(parse("console.log(console.log(\"a\"));"));
    CallExpression inner = (CallExpression) outer.arguments().get(0);

    assertEquals(
        new MemberExpression(new Identifier("console"), "log"), inner.callee());
    assertEquals(List.of(new StringLiteral("a")), inner.arguments());
  }

  @Test
  void parsesAddition() {
    ExpressionStatement statement = (ExpressionStatement) parse("a + b;").statements().get(0);

    assertEquals(
        new BinaryExpression(new Identifier("a"), "+", new Identifier("b")),
        statement.expression());
  }

  @Test
  void parsesAdditionAsCallArgument() {
    CallExpression call = firstCall(parse("console.log(2 + 2);"));

    assertEquals(
        List.of(
            new BinaryExpression(new NumericLiteral("2"), "+", new NumericLiteral("2"))),
        call.arguments());
  }

  @Test
  void parsesParenthesizedExpression() {
    ExpressionStatement statement = (ExpressionStatement) parse("(a + b);").statements().get(0);

    assertEquals(
        new BinaryExpression(new Identifier("a"), "+", new Identifier("b")),
        statement.expression());
  }

  private static CallExpression firstCall(Program program) {
    return (CallExpression) ((ExpressionStatement) program.statements().get(0)).expression();
  }
}
