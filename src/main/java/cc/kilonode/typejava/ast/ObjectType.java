package cc.kilonode.typejava.ast;

import java.util.List;

public record ObjectType(List<TypeField> fields) implements TypeReference {
  public ObjectType {
    fields = List.copyOf(fields);
  }
}
