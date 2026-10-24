package cc.kilonode.typejava.ast;

import java.util.List;

public record FunctionType(List<Parameter> parameters, TypeReference returnType)
    implements TypeReference {
  public FunctionType {
    parameters = List.copyOf(parameters);
  }
}
