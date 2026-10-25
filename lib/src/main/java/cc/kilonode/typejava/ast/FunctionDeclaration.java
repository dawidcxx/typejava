package cc.kilonode.typejava.ast;

import java.util.List;

public record FunctionDeclaration(
    String name,
    List<Parameter> parameters,
    TypeReference returnType,
    BlockStatement body,
    boolean exported)
    implements Statement {
  public FunctionDeclaration {
    parameters = List.copyOf(parameters);
  }
}
