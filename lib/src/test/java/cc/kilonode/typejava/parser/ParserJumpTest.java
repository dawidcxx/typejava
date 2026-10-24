package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static cc.kilonode.typejava.testing.TestFixtures.read;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import cc.kilonode.typejava.ast.BreakStatement;
import cc.kilonode.typejava.ast.ContinueStatement;
import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.IfStatement;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.WhileStatement;
import org.junit.jupiter.api.Test;

class ParserJumpTest {
  @Test
  void parsesBreakStatement() {
    Program program = parse("break;");

    assertEquals(new BreakStatement(), program.statements().get(0));
  }

  @Test
  void parsesContinueStatement() {
    Program program = parse("continue;");

    assertEquals(new ContinueStatement(), program.statements().get(0));
  }

  @Test
  void parsesJumpsInsideWhile() {
    Program program = parse("while (true) { break; continue; }");

    WhileStatement loop = (WhileStatement) program.statements().get(0);
    assertInstanceOf(BreakStatement.class, loop.body().statements().get(0));
    assertInstanceOf(ContinueStatement.class, loop.body().statements().get(1));
  }

  @Test
  void parsesLoopJumpFixture() {
    Program program = parse(read("loop-jumps.tj"));

    FunctionDeclaration function = (FunctionDeclaration) program.statements().get(0);
    WhileStatement loop = (WhileStatement) function.body().statements().get(1);
    assertInstanceOf(IfStatement.class, loop.body().statements().get(0));
    assertInstanceOf(IfStatement.class, loop.body().statements().get(1));
  }
}
