package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static cc.kilonode.typejava.testing.TestFixtures.read;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import cc.kilonode.typejava.ast.BlockStatement;
import cc.kilonode.typejava.ast.BooleanLiteral;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.VariableDeclaration;
import cc.kilonode.typejava.ast.WhileStatement;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserLoopTest {
  @Test
  void parsesWhileStatement() {
    Program program = parse("while (true) {}");

    assertEquals(
        new WhileStatement(new BooleanLiteral(true), new BlockStatement(List.of())),
        program.statements().get(0));
  }

  @Test
  void parsesWhileBody() {
    Program program = parse("while (i < 10) { i = i + 1; }");

    WhileStatement loop = (WhileStatement) program.statements().get(0);
    assertEquals(1, loop.body().statements().size());
    assertInstanceOf(ExpressionStatement.class, loop.body().statements().get(0));
  }

  @Test
  void parsesLoopFixture() {
    Program program = parse(read("loops.tj"));

    FunctionDeclaration function = (FunctionDeclaration) program.statements().get(0);
    assertEquals(3, function.body().statements().size());
    assertInstanceOf(VariableDeclaration.class, function.body().statements().get(0));
    assertInstanceOf(WhileStatement.class, function.body().statements().get(1));
    assertInstanceOf(ReturnStatement.class, function.body().statements().get(2));
  }
}
