package cc.kilonode.typejava.ast;

import java.util.List;

public record UnionType(List<TypeReference> members) implements TypeReference {
  public UnionType {
    members = List.copyOf(members);
  }
}
