package cc.kilonode.typejava;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public record ExportSignature(
    String name, List<Class<?>> parameterTypes, Class<?> returnType) {
  public ExportSignature {
    parameterTypes = List.copyOf(parameterTypes);
  }

  boolean matches(Method method) {
    return Arrays.equals(method.getParameterTypes(), parameterTypes.toArray(Class<?>[]::new))
        && method.getReturnType().equals(returnType);
  }
}
