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

class ParserTest {
  @Test
  void parsesConsoleLogCall() {
    Program program = parse("console.log(\"hello world\");");

    Program expected =
        new Program(
            List.of(
                new ExpressionStatement(
                    new CallExpression(
                        new MemberExpression(new Identifier("console"), "log"),
                        List.of(new StringLiteral("hello world"))))));

    assertEquals(expected, program);
  }

  @Test
  void parsesMultipleStatements() {
    Program program = parse("console.log(\"a\");\nconsole.log(\"b\");");

    assertEquals(2, program.statements().size());
  }

  @Test
  void parsesEmptyProgram() {
    assertEquals(new Program(List.of()), parse(""));
  }
}
