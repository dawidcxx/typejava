package cc.kilonode.typejava.checker;

import cc.kilonode.typejava.ast.Expression;
import cc.kilonode.typejava.ast.FunctionExpression;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ObjectLiteralInfo {
  private final Type.Nominal nominalType;
  private final ObjectTypeInfo objectType;
  private final List<Type> typeArguments;
  private final Map<String, Expression> values = new LinkedHashMap<>();
  private final Map<String, FunctionExpression> methods = new LinkedHashMap<>();

  public ObjectLiteralInfo(
      Type.Nominal nominalType, ObjectTypeInfo objectType, List<Type> typeArguments) {
    this.nominalType = nominalType;
    this.objectType = objectType;
    this.typeArguments = List.copyOf(typeArguments);
  }

  public Type.Nominal nominalType() {
    return nominalType;
  }

  public ObjectTypeInfo objectType() {
    return objectType;
  }

  public List<Type> typeArguments() {
    return typeArguments;
  }

  public Map<String, Expression> values() {
    return values;
  }

  public Map<String, FunctionExpression> methods() {
    return methods;
  }
}
