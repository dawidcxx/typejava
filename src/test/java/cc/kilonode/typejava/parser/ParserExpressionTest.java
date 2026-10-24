package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.StringLiteral;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserExpressionTest {
  @Test
  void parsesMultipleArguments() {
    Program program = parse("console.log(\"a\", \"b\");");

    CallExpression call = (CallExpression) ((ExpressionStatement) program.statements().get(0)).expression();
    assertEquals(List.of(new StringLiteral("a"), new StringLiteral("b")), call.arguments());
  }

  @Test
  void parsesEmptyArgumentList() {
    Program program = parse("console.log();");

    CallExpression call = (CallExpression) ((ExpressionStatement) program.statements().get(0)).expression();
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
    Program program = parse("console.log(console.log(\"a\"));");

    CallExpression outer =
        (CallExpression) ((ExpressionStatement) program.statements().get(0)).expression();
    CallExpression inner = (CallExpression) outer.arguments().get(0);

    assertEquals(
        new MemberExpression(new Identifier("console"), "log"), inner.callee());
    assertEquals(List.of(new StringLiteral("a")), inner.arguments());
  }
}
