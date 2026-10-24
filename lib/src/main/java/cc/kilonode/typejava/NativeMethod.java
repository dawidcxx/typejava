package cc.kilonode.typejava;

import cc.kilonode.typejava.checker.Type;
import java.util.List;

public record NativeMethod(
    String name,
    String descriptor,
    Type returnType,
    List<Type> parameterTypes,
    boolean isStatic,
    boolean isFactory,
    boolean injectsEnvironment) {
  public NativeMethod {
    parameterTypes = List.copyOf(parameterTypes);
  }
}
