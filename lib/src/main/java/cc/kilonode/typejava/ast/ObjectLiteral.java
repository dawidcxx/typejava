package cc.kilonode.typejava.ast;

import java.util.List;

public record ObjectLiteral(List<ObjectField> fields) implements Expression {
  public ObjectLiteral {
    fields = List.copyOf(fields);
  }
}
