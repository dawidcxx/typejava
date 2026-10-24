package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static cc.kilonode.typejava.testing.TestFixtures.read;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.FunctionExpression;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.MethodSignature;
import cc.kilonode.typejava.ast.NamedType;
import cc.kilonode.typejava.ast.ObjectLiteral;
import cc.kilonode.typejava.ast.ObjectType;
import cc.kilonode.typejava.ast.Parameter;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.StringLiteral;
import cc.kilonode.typejava.ast.VariableDeclaration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserMethodTest {
  @Test
  void parsesMethodSignatureInObjectType() {
    Program program = parse("type Person = { name: string; greet(this: Person): string; };");

    ObjectType person = (ObjectType) program.typeDeclarations().get(0).type();
    MethodSignature greet = (MethodSignature) person.members().get(1);
    assertEquals("greet", greet.name());
    assertEquals(
        List.of(new Parameter("this", new NamedType("Person"))), greet.parameters());
    assertEquals(new NamedType("string"), greet.returnType());
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
