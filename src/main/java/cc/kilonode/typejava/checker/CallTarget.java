package cc.kilonode.typejava.checker;

import java.util.List;

public sealed interface CallTarget {
  enum Builtin {
    ARRAY,
    MAP
  }

  record StaticFunction(FunctionSymbol symbol) implements CallTarget {}

  record ObjectMethod(
      Type receiverType,
      ObjectTypeInfo owner,
      FieldInfo field,
      Type.Function declaredType,
      Type.Function resolvedType)
      implements CallTarget {}

  record ConsoleLog() implements CallTarget {}

  record BuiltinCreate(Builtin builtin, Type type) implements CallTarget {}

  record BuiltinMethod(
      Builtin builtin, String name, Type receiverType, Type returnType, List<Type> parameterTypes)
      implements CallTarget {
    public BuiltinMethod {
      parameterTypes = List.copyOf(parameterTypes);
    }
  }
}
