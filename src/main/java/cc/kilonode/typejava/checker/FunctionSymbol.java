package cc.kilonode.typejava.checker;

import cc.kilonode.typejava.ast.FunctionDeclaration;
import java.util.List;

public record FunctionSymbol(
    String name,
    List<Type> parameterTypes,
    Type returnType,
    FunctionDeclaration declaration) {
  public FunctionSymbol {
    parameterTypes = List.copyOf(parameterTypes);
  }
}
