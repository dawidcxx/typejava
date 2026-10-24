package cc.kilonode.typejava.ast;

import java.util.List;

public record TypeAliasDeclaration(
    String name, List<String> typeParameters, TypeReference type) {
  public TypeAliasDeclaration {
    typeParameters = List.copyOf(typeParameters);
  }

  public TypeAliasDeclaration(String name, TypeReference type) {
    this(name, List.of(), type);
  }
}
