package cc.kilonode.typejava.ast;

import java.util.List;

public record ObjectType(List<TypeField> members) implements TypeReference {
  public ObjectType {
    members = List.copyOf(members);
  }
}
