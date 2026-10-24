package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import cc.kilonode.typejava.ast.BlockStatement;
import cc.kilonode.typejava.ast.BooleanLiteral;
import cc.kilonode.typejava.ast.IfStatement;
import cc.kilonode.typejava.ast.Program;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserBranchTest {
  @Test
  void parsesIfStatement() {
    Program program = parse("if (true) {}");

    IfStatement statement = (IfStatement) program.statements().get(0);
    assertEquals(new BooleanLiteral(true), statement.condition());
    assertEquals(new BlockStatement(List.of()), statement.thenBranch());
    assertNull(statement.elseBranch());
  }

  @Test
  void parsesIfElseStatement() {
    Program program = parse("if (true) {} else {}");

    IfStatement statement = (IfStatement) program.statements().get(0);
    assertEquals(new BlockStatement(List.of()), statement.elseBranch());
  }

  @Test
  void parsesElseIfChain() {
    Program program = parse("if (true) {} else if (false) {} else {}");

    IfStatement statement = (IfStatement) program.statements().get(0);
    IfStatement elseIf = (IfStatement) statement.elseBranch();
    assertEquals(new BooleanLiteral(false), elseIf.condition());
    assertEquals(new BlockStatement(List.of()), elseIf.elseBranch());
  }
}
