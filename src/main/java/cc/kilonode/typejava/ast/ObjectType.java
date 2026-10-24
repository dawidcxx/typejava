package cc.kilonode.typejava.ast;

import java.util.List;

public record ObjectType(List<TypeMember> members) implements TypeReference {
  public ObjectType {
    members = List.copyOf(members);
  }
}
