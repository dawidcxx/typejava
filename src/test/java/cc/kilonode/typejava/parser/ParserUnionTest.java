package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static cc.kilonode.typejava.testing.TestFixtures.read;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.IfStatement;
import cc.kilonode.typejava.ast.NamedType;
import cc.kilonode.typejava.ast.NullLiteral;
import cc.kilonode.typejava.ast.NullType;
import cc.kilonode.typejava.ast.ObjectType;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.TypeAliasDeclaration;
import cc.kilonode.typejava.ast.TypeField;
import cc.kilonode.typejava.ast.UnionType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserUnionTest {
  @Test
  void parsesNullableTypeAlias() {
    Program program = parse("type MaybeNumber = number | null;");

    assertEquals(
        new TypeAliasDeclaration(
            "MaybeNumber",
            new UnionType(List.of(new NamedType("number"), new NullType()))),
        program.typeDeclarations().get(0));
  }

  @Test
  void parsesUnionWithObjectType() {
    Program program = parse("type Shape = { radius: number; } | null;");

    UnionType type = (UnionType) program.typeDeclarations().get(0).type();
    assertEquals(2, type.members().size());
    assertEquals(
        new ObjectType(List.of(new TypeField("radius", false, new NamedType("number")))),
        type.members().get(0));
    assertEquals(new NullType(), type.members().get(1));
  }

  @Test
  void parsesNullLiteral() {
    Program program = parse("null;");

    assertEquals(
        new ExpressionStatement(new NullLiteral()), program.statements().get(0));
  }

  @Test
  void parsesNullableTypesInFixture() {
    Program program = parse(read("unions.tj"));

    TypeAliasDeclaration person = program.typeDeclarations().get(0);
    TypeField age = (TypeField) ((ObjectType) person.type()).members().get(1);
    assertEquals(
        new UnionType(List.of(new NamedType("number"), new NullType())), age.type());

    TypeAliasDeclaration personOrNull = program.typeDeclarations().get(1);
    assertEquals(
        new UnionType(List.of(new NamedType("Person"), new NullType())),
        personOrNull.type());

    FunctionDeclaration ageOf = (FunctionDeclaration) program.statements().get(0);
    assertEquals(new NamedType("PersonOrNull"), ageOf.parameters().get(0).type());
    assertEquals(
        new UnionType(List.of(new NamedType("number"), new NullType())),
        ageOf.returnType());

    IfStatement guard = (IfStatement) ageOf.body().statements().get(0);
    ReturnStatement nullReturn = (ReturnStatement) guard.thenBranch().statements().get(0);
    assertEquals(new NullLiteral(), nullReturn.value());
  }
}
