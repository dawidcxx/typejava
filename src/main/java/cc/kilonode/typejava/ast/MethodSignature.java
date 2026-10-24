package cc.kilonode.typejava.ast;

import java.util.List;

public record MethodSignature(
    String name, List<Parameter> parameters, TypeReference returnType) implements TypeMember {
  public MethodSignature {
    parameters = List.copyOf(parameters);
  }
}
