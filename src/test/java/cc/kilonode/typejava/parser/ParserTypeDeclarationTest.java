package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static cc.kilonode.typejava.testing.TestFixtures.read;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.NamedType;
import cc.kilonode.typejava.ast.ObjectType;
import cc.kilonode.typejava.ast.Parameter;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.TypeAliasDeclaration;
import cc.kilonode.typejava.ast.TypeField;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserTypeDeclarationTest {
  @Test
  void parsesNestedObjectTypeAlias() {
    Program program = parse(read("custom-types.tj"));

    TypeAliasDeclaration expected =
        new TypeAliasDeclaration(
            "MyCustomType",
            new ObjectType(
                List.of(
                    new TypeField("a", new NamedType("number")),
                    new TypeField("b", new NamedType("string")),
                    new TypeField(
                        "c",
                        new ObjectType(
                            List.of(new TypeField("g", new NamedType("void"))))))));

    assertEquals(new Program(List.of(expected), List.of()), program);
  }

  @Test
  void parsesEmptyObjectType() {
    Program program = parse("type Empty = {};");

    assertEquals(
        new TypeAliasDeclaration("Empty", new ObjectType(List.of())),
        program.typeDeclarations().get(0));
  }

  @Test
  void parsesNamedTypeInFunctionSignature() {
    Program program =
        parse("type Payload = { value: number; }; function read(p: Payload): void {};");

    FunctionDeclaration function = (FunctionDeclaration) program.statements().get(0);
    assertEquals(
        List.of(new Parameter("p", new NamedType("Payload"))), function.parameters());
    assertEquals(new NamedType("void"), function.returnType());
  }

  @Test
  void parsesInlineObjectTypeInFunctionSignature() {
    Program program =
        parse(
            "function build(input: { value: number; }): { result: string; } {"
                + " let result: string = \"ok\"; return result; };");

    FunctionDeclaration function = (FunctionDeclaration) program.statements().get(0);
    assertEquals(
        new ObjectType(List.of(new TypeField("value", new NamedType("number")))),
        function.parameters().get(0).type());
    assertEquals(
        new ObjectType(List.of(new TypeField("result", new NamedType("string")))),
        function.returnType());
    assertEquals(2, function.body().statements().size());
  }
}
