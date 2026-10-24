package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parse;
import static cc.kilonode.typejava.testing.TestFixtures.read;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.NamedType;
import cc.kilonode.typejava.ast.ObjectType;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.TypeAliasDeclaration;
import cc.kilonode.typejava.ast.TypeField;
import cc.kilonode.typejava.ast.VariableDeclaration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserGenericTest {
  @Test
  void parsesGenericTypeAlias() {
    Program program = parse("type Box<T> = { value: T; };");

    assertEquals(
        new TypeAliasDeclaration(
            "Box",
            List.of("T"),
            new ObjectType(List.of(new TypeField("value", false, new NamedType("T"))))),
        program.typeDeclarations().get(0));
  }

  @Test
  void parsesMultipleTypeParameters() {
    Program program = parse("type Pair<K, V> = { key: K; value: V; };");

    TypeAliasDeclaration pair = program.typeDeclarations().get(0);
    assertEquals(List.of("K", "V"), pair.typeParameters());
    assertEquals(
        new ObjectType(
            List.of(
                new TypeField("key", false, new NamedType("K")),
                new TypeField("value", false, new NamedType("V")))),
        pair.type());
  }

  @Test
  void parsesNestedTypeArguments() {
    Program program = parse("type Registry<T> = TjMap<string, TjArray<T>>;");

    TypeAliasDeclaration registry = program.typeDeclarations().get(0);
    assertEquals(
        new NamedType(
            "TjMap",
            List.of(
                new NamedType("string"),
                new NamedType("TjArray", List.of(new NamedType("T"))))),
        registry.type());
  }

  @Test
  void parsesGenericDeclarationWithoutSpaceBeforeEquals() {
    Program program = parse("type Box<T>= { value: T; }; let boxed: Box<number>= null;");

    TypeAliasDeclaration box = program.typeDeclarations().get(0);
    assertEquals(List.of("T"), box.typeParameters());

    VariableDeclaration boxed = (VariableDeclaration) program.statements().get(0);
    assertEquals(new NamedType("Box", List.of(new NamedType("number"))), boxed.type());
  }

  @Test
  void parsesNestedTypeArgumentsWithoutSpaceBeforeEquals() {
    Program program = parse("let registry: TjMap<string, TjArray<number>>= null;");

    VariableDeclaration registry = (VariableDeclaration) program.statements().get(0);
    assertEquals(
        new NamedType(
            "TjMap",
            List.of(
                new NamedType("string"),
                new NamedType("TjArray", List.of(new NamedType("number"))))),
        registry.type());
  }

  @Test
  void parsesGenericFixture() {
    Program program = parse(read("generics.tj"));

    TypeAliasDeclaration box = program.typeDeclarations().get(0);
    assertEquals(List.of("T"), box.typeParameters());

    TypeAliasDeclaration registry = program.typeDeclarations().get(1);
    assertEquals(List.of("T"), registry.typeParameters());

    VariableDeclaration boxed = (VariableDeclaration) program.statements().get(0);
    assertEquals(
        new NamedType("Box", List.of(new NamedType("UpstreamLog"))), boxed.type());

    VariableDeclaration stored = (VariableDeclaration) program.statements().get(1);
    assertEquals(
        new NamedType("Registry", List.of(new NamedType("UpstreamLog"))), stored.type());
  }
}
