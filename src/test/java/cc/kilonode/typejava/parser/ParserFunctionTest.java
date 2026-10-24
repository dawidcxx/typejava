package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.BlockStatement;
import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.Parameter;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.TypeReference;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserFunctionTest {
  @Test
  void parsesFunctionWithNoParameters() {
    Program program = parse("function ping(): number { return 1; };");

    FunctionDeclaration function = (FunctionDeclaration) program.statements().get(0);
    assertEquals("ping", function.name());
    assertEquals(List.of(), function.parameters());
    assertEquals(new TypeReference("number"), function.returnType());
    assertEquals(
        new BlockStatement(List.of(new ReturnStatement(new NumericLiteral("1")))),
        function.body());
  }

  @Test
  void keepsParameterNamesAndTypes() {
    Program program = parse("function add(a: number, b: string): number { return a; };");

    FunctionDeclaration function = (FunctionDeclaration) program.statements().get(0);
    assertEquals(
        List.of(
            new Parameter("a", new TypeReference("number")),
            new Parameter("b", new TypeReference("string"))),
        function.parameters());
  }

  @Test
  void parsesFunctionWithEmptyBody() {
    Program program = parse("function noop(): number {};");

    FunctionDeclaration function = (FunctionDeclaration) program.statements().get(0);
    assertEquals(new BlockStatement(List.of()), function.body());
  }

}
