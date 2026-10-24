package cc.kilonode.typejava.ast;

import java.util.List;

public record FunctionExpression(
    List<Parameter> parameters, TypeReference returnType, BlockStatement body)
    implements Expression {
  public FunctionExpression {
    parameters = List.copyOf(parameters);
  }
}
