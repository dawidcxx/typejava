package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.BlockStatement;
import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.Parameter;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.TypeReference;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserProgramTest {
  @Test
  void parsesTypedFunctionAndCall() {
    Program program =
        parse(
            """
            function add(a: number, b: number): number {
              return a + b;
            };

            console.log(add(2, 2));
            """);

    Program expected =
        new Program(
            List.of(
                new FunctionDeclaration(
                    "add",
                    List.of(
                        new Parameter("a", new TypeReference("number")),
                        new Parameter("b", new TypeReference("number"))),
                    new TypeReference("number"),
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
