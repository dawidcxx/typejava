package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static cc.kilonode.typejava.testing.TestFixtures.read;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.BlockStatement;
import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.IfStatement;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.Parameter;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.NamedType;
import cc.kilonode.typejava.ast.VariableDeclaration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserProgramTest {
  @Test
  void parsesTypedFunctionAndCall() {
    Program program = parse(read("basic-function.tj"));

    Program expected =
        new Program(
            List.of(),
            List.of(
                new FunctionDeclaration(
                    "add",
                    List.of(
                        new Parameter("a", new NamedType("number")),
                        new Parameter("b", new NamedType("number"))),
                    new NamedType("number"),
                    new BlockStatement(
                        List.of(
                            new ReturnStatement(
                                new BinaryExpression(
                                    new Identifier("a"), "+", new Identifier("b")))))),
                new ExpressionStatement(
                    new CallExpression(
                        new MemberExpression(new Identifier("console"), "log"),
                        List.of(
                            new CallExpression(
                                new Identifier("add"),
                                List.of(
                                    new NumericLiteral("2"), new NumericLiteral("2"))))))));

    assertEquals(expected, program);
  }

  @Test
  void parsesLocalsAndBranchesFixture() {
    Program program = parse(read("locals-and-branches.tj"));

    FunctionDeclaration function = (FunctionDeclaration) program.statements().get(0);
    assertEquals(5, function.body().statements().size());
    assertInstanceOf(VariableDeclaration.class, function.body().statements().get(0));
    assertInstanceOf(VariableDeclaration.class, function.body().statements().get(1));
    assertInstanceOf(VariableDeclaration.class, function.body().statements().get(2));
    assertInstanceOf(IfStatement.class, function.body().statements().get(3));
    assertInstanceOf(ReturnStatement.class, function.body().statements().get(4));
    assertEquals(2, program.statements().size());
  }

  @Test
  void parsesMultipleFunctionDeclarations() {
    Program program =
        parse(
            """
            function first(): number { return 1; };
            function second(): number { return 2; };
            """);

    assertEquals(2, program.statements().size());
    assertEquals(
        List.of("first", "second"),
        program.statements().stream()
            .map(statement -> ((FunctionDeclaration) statement).name())
            .toList());
  }
}
