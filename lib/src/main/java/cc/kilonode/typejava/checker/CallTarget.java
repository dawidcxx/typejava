package cc.kilonode.typejava.checker;

import cc.kilonode.typejava.NativeMethod;
import cc.kilonode.typejava.NativeType;
import java.util.List;

public sealed interface CallTarget {
  record StaticFunction(FunctionSymbol symbol) implements CallTarget {}

  record ObjectMethod(
      Type receiverType,
      ObjectTypeInfo owner,
      FieldInfo field,
      Type.Function declaredType,
      Type.Function resolvedType)
      implements CallTarget {}

  record ConsoleLog() implements CallTarget {}

  record NativeCall(
      NativeType type, NativeMethod method, Type receiverType, Type returnType)
      implements CallTarget {}
}
