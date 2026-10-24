package cc.kilonode.typejava.ast;

import java.util.List;

public record NamedType(String name, List<TypeReference> typeArguments) implements TypeReference {
  public NamedType {
    typeArguments = List.copyOf(typeArguments);
  }

  public NamedType(String name) {
    this(name, List.of());
  }
}
