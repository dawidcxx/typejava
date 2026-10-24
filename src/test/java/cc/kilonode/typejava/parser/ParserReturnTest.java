package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import cc.kilonode.typejava.ast.BlockStatement;
import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.IfStatement;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.ReturnStatement;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserReturnTest {
  @Test
  void parsesBareReturn() {
    Program program = parse("function noop(): void { return; };");

    FunctionDeclaration function = (FunctionDeclaration) program.statements().get(0);
    ReturnStatement statement = (ReturnStatement) function.body().statements().get(0);
    assertNull(statement.value());
  }

  @Test
  void parsesReturnWithValue() {
    Program program = parse("function value(): number { return 1; };");

    FunctionDeclaration function = (FunctionDeclaration) program.statements().get(0);
    ReturnStatement statement = (ReturnStatement) function.body().statements().get(0);
    assertEquals(new NumericLiteral("1"), statement.value());
  }

  @Test
  void parsesBareReturnInsideIf() {
    Program program = parse("if (done) { return; }");

    IfStatement ifStatement = (IfStatement) program.statements().get(0);
    assertEquals(new Identifier("done"), ifStatement.condition());
    assertEquals(
        new BlockStatement(List.of(new ReturnStatement(null))), ifStatement.thenBranch());
  }
}
