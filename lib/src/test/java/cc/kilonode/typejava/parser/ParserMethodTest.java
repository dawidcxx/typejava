package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static cc.kilonode.typejava.testing.TestFixtures.read;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.FunctionExpression;
import cc.kilonode.typejava.ast.FunctionType;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.NamedType;
import cc.kilonode.typejava.ast.NullType;
import cc.kilonode.typejava.ast.ObjectLiteral;
import cc.kilonode.typejava.ast.ObjectType;
import cc.kilonode.typejava.ast.Parameter;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.StringLiteral;
import cc.kilonode.typejava.ast.TypeField;
import cc.kilonode.typejava.ast.UnionType;
import cc.kilonode.typejava.ast.VariableDeclaration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserMethodTest {
  @Test
  void parsesFunctionTypeFieldInObjectType() {
    Program program =
        parse("type Person = { name: string; greet: (this: Person) => string; };");

    ObjectType person = (ObjectType) program.typeDeclarations().get(0).type();
    TypeField greet = person.members().get(1);
    assertEquals("greet", greet.name());
    assertEquals(
        new FunctionType(
            List.of(new Parameter("this", new NamedType("Person"))),
            new NamedType("string")),
        greet.type());
  }

  @Test
  void parsesEmptyFunctionType() {
    Program program = parse("let callback: () => void = null;");

    VariableDeclaration callback = (VariableDeclaration) program.statements().get(0);
    assertEquals(
        new FunctionType(List.of(), new NamedType("void")), callback.type());
  }

  @Test
  void parsesNullableFunctionType() {
    Program program =
        parse("type Handler = { callback: ((value: number) => string) | null; };");

    ObjectType handler = (ObjectType) program.typeDeclarations().get(0).type();
    assertEquals(
        new UnionType(
            List.of(
                new FunctionType(
                    List.of(new Parameter("value", new NamedType("number"))),
                    new NamedType("string")),
                new NullType())),
        handler.members().get(0).type());
  }

  @Test
  void parsesObjectMethodAndCall() {
    Program program = parse(read("person.tj"));

    VariableDeclaration declaration = (VariableDeclaration) program.statements().get(0);
    ObjectLiteral alice = (ObjectLiteral) declaration.initializer();
    FunctionExpression greet = (FunctionExpression) alice.fields().get(2).value();

    assertEquals(
        List.of(new Parameter("this", new NamedType("Person"))), greet.parameters());
    assertEquals(new NamedType("string"), greet.returnType());
    ReturnStatement returnStatement = (ReturnStatement) greet.body().statements().get(0);
    assertEquals(
        new BinaryExpression(
            new StringLiteral("Hi, I'm "),
            "+",
            new MemberExpression(new Identifier("this"), "name")),
        returnStatement.value());

    ExpressionStatement callStatement = (ExpressionStatement) program.statements().get(1);
    assertEquals(
        new CallExpression(
            new MemberExpression(new Identifier("console"), "log"),
            List.of(
                new CallExpression(
                    new MemberExpression(new Identifier("alice"), "greet"),
                    List.of()))),
        callStatement.expression());
  }

  @Test
  void parsesThisMemberAccess() {
    Program program = parse("this.name;");

    ExpressionStatement statement = (ExpressionStatement) program.statements().get(0);
    assertEquals(
        new MemberExpression(new Identifier("this"), "name"), statement.expression());
  }
}
