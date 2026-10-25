package cc.kilonode.typejava;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Map;

public final class ProgramInstance {
  private final Object target;
  private final Environment environment;
  private final Map<String, ExportedFunction> exports;

  ProgramInstance(
      Object target, Environment environment, Map<String, ExportedFunction> exports) {
    this.target = target;
    this.environment = environment;
    this.exports = exports;
  }

  public Object invoke(String name, Object... arguments) {
    ExportedFunction function = exports.get(name);
    if (function == null) {
      throw new IllegalArgumentException("Unknown exported function '" + name + "'");
    }
    Object[] values = arguments == null ? new Object[0] : arguments;
    if (values.length != function.signature().parameterTypes().size()) {
      throw new IllegalArgumentException(
          "Exported function '"
              + name
              + "' expects "
              + function.signature().parameterTypes().size()
              + " argument(s) but found "
              + values.length);
    }
    Object[] invocation = new Object[values.length + 1];
    invocation[0] = environment;
    System.arraycopy(values, 0, invocation, 1, values.length);
    try {
      return function.method().invoke(target, invocation);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new RuntimeException(cause);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(
          "Could not invoke exported TypeJava function '" + name + "'", exception);
    }
  }

  public <T> T exports(Class<T> api) {
    if (!api.isInterface()) {
      throw new IllegalArgumentException(api.getName() + " is not an interface");
    }
    for (Method method : api.getMethods()) {
      if (method.getDeclaringClass() == Object.class
          || Modifier.isStatic(method.getModifiers())
          || method.isDefault()) {
        continue;
      }
      ExportedFunction function = exports.get(method.getName());
      if (function == null) {
        throw new IllegalArgumentException(
            "Export contract "
                + api.getName()
                + "."
                + method.getName()
                + " has no matching exported function");
      }
      if (!function.signature().matches(method)) {
        throw new IllegalArgumentException(
            "Export contract "
                + api.getName()
                + "."
                + method.getName()
                + " does not match the exported function signature");
      }
    }

    InvocationHandler handler =
        (proxy, method, arguments) -> {
          if (method.getDeclaringClass() == Object.class) {
            return invokeObjectMethod(proxy, method, arguments);
          }
          if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, arguments);
          }
          return invoke(method.getName(), arguments);
        };
    ClassLoader loader = api.getClassLoader();
    if (loader == null) {
      loader = TypeJava.class.getClassLoader();
    }
    return api.cast(Proxy.newProxyInstance(loader, new Class<?>[] {api}, handler));
  }

  private Object invokeObjectMethod(Object proxy, Method method, Object[] arguments) {
    return switch (method.getName()) {
      case "toString" -> "ProgramInstance[" + exports.keySet() + "]";
      case "hashCode" -> System.identityHashCode(proxy);
      case "equals" -> proxy == arguments[0];
      default -> throw new IllegalStateException(method.getName());
    };
  }
}
