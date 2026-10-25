package cc.kilonode.typejava;

import cc.kilonode.typejava.checker.Type;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class TypeScriptDeclarations {
  private static final Comparator<Method> METHOD_ORDER =
      Comparator.comparing(Method::getName)
          .thenComparing(method -> Arrays.toString(method.getParameterTypes()))
          .thenComparing(method -> method.getReturnType().getName());

  private TypeScriptDeclarations() {}

  static String generate(Environment environment) {
    StringBuilder declarations = new StringBuilder();
    appendNativeTypes(declarations, environment);
    appendRecords(declarations, environment);
    appendExportInterfaces(declarations, environment);
    return declarations.toString();
  }

  private static void appendNativeTypes(StringBuilder declarations, Environment environment) {
    List<NativeType> types = new ArrayList<>(environment.types());
    types.sort(Comparator.comparing(NativeType::name));
    for (NativeType type : types) {
      declarations
          .append("interface ")
          .append(type.name())
          .append(renderParameters(type.parameterNames()))
          .append(" {\n");
      for (NativeMethod method : type.methods()) {
        if (method.isStatic()) {
          continue;
        }
        declarations
            .append("  ")
            .append(method.name())
            .append(renderArguments(method.parameterTypes()))
            .append(": ")
            .append(render(method.returnType()))
            .append(";\n");
      }
      declarations.append("}\n\n");

      List<NativeMethod> staticMethods =
          type.methods().stream().filter(NativeMethod::isStatic).toList();
      if (!staticMethods.isEmpty()) {
        declarations.append("declare const ").append(type.name()).append(": {\n");
        for (NativeMethod method : staticMethods) {
          Set<String> variables = usedVariables(method);
          List<String> methodParameters =
              type.parameterNames().stream().filter(variables::contains).toList();
          declarations
              .append("  ")
              .append(method.name())
              .append(renderParameters(methodParameters))
              .append(renderArguments(method.parameterTypes()))
              .append(": ")
              .append(render(method.returnType()))
              .append(";\n");
        }
        declarations.append("};\n\n");
      }
    }
  }

  private static void appendRecords(StringBuilder declarations, Environment environment) {
    List<RecordType> records = new ArrayList<>(environment.records());
    records.sort(Comparator.comparing(RecordType::name));
    for (RecordType record : records) {
      declarations.append("interface ").append(record.name()).append(" {\n");
      for (RecordType.Field field : record.fields()) {
        declarations
            .append("  readonly ")
            .append(field.name())
            .append(": ")
            .append(render(field.type()))
            .append(";\n");
      }
      declarations.append("}\n\n");
    }
  }

  private static void appendExportInterfaces(
      StringBuilder declarations, Environment environment) {
    for (Class<?> api : environment.exportInterfaces()) {
      declarations.append("interface ").append(api.getSimpleName()).append(" {\n");
      List<Method> methods =
          Arrays.stream(api.getMethods())
              .filter(
                  method ->
                      method.getDeclaringClass() != Object.class
                          && !Modifier.isStatic(method.getModifiers())
                          && !method.isDefault())
              .sorted(METHOD_ORDER)
              .toList();
      for (Method method : methods) {
        declarations.append("  ").append(method.getName()).append("(");
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
          if (i > 0) {
            declarations.append(", ");
          }
          Parameter parameter = parameters[i];
          String name = parameter.isNamePresent() ? parameter.getName() : "arg" + i;
          declarations
              .append(name)
              .append(": ")
              .append(
                  renderHostType(
                      parameter.getType(),
                      parameter.getAnnotation(MaybeNull.class) != null,
                      environment));
        }
        declarations
            .append("): ")
            .append(
                renderHostType(
                    method.getReturnType(),
                    method.getAnnotation(MaybeNull.class) != null,
                    environment))
            .append(";\n");
      }
      declarations.append("}\n\n");
    }
  }

  private static String renderHostType(
      Class<?> type, boolean maybeNull, Environment environment) {
    String rendered;
    boolean nullable = maybeNull;
    if (type == void.class || type == Void.class) {
      rendered = "void";
    } else if (type == double.class || type == int.class) {
      rendered = "number";
    } else if (type == Double.class) {
      rendered = "number";
      nullable = true;
    } else if (type == boolean.class) {
      rendered = "boolean";
    } else if (type == Boolean.class) {
      rendered = "boolean";
      nullable = true;
    } else if (type == String.class) {
      rendered = "string";
    } else {
      RecordType record = recordFor(type, environment);
      if (record != null) {
        rendered = record.name();
      } else {
        NativeType nativeType = nativeTypeFor(type, environment);
        if (nativeType == null) {
          throw new IllegalArgumentException(
              "Type " + type.getName() + " cannot be represented in TypeScript declarations");
        }
        rendered =
            nativeType.name()
                + renderParameters(
                    Collections.nCopies(nativeType.parameterNames().size(), "unknown"));
      }
    }
    return nullable ? rendered + " | null" : rendered;
  }

  private static RecordType recordFor(Class<?> type, Environment environment) {
    for (RecordType record : environment.records()) {
      if (record.javaClass().equals(type)) {
        return record;
      }
    }
    return null;
  }

  private static NativeType nativeTypeFor(Class<?> type, Environment environment) {
    for (NativeType nativeType : environment.types()) {
      if (nativeType.javaClass().equals(type)) {
        return nativeType;
      }
    }
    return null;
  }

  private static String render(Type type) {
    if (type instanceof Type.Primitive primitive) {
      return switch (primitive.kind()) {
        case NUMBER -> "number";
        case STRING -> "string";
        case BOOLEAN -> "boolean";
        case VOID -> "void";
      };
    }
    if (type instanceof Type.Null) {
      return "null";
    }
    if (type instanceof Type.Nominal nominal) {
      return nominal.name() + renderTypeArguments(nominal.typeArguments());
    }
    if (type instanceof Type.Nullable nullable) {
      String inner = render(nullable.inner());
      if (nullable.inner() instanceof Type.Function) {
        inner = "(" + inner + ")";
      }
      return inner + " | null";
    }
    if (type instanceof Type.Variable variable) {
      return variable.name();
    }
    if (type instanceof Type.Function function) {
      List<String> parameters = new ArrayList<>();
      if (function.receiver() != null) {
        parameters.add("this: " + render(function.receiver()));
      }
      for (int i = 0; i < function.parameters().size(); i++) {
        parameters.add("arg" + i + ": " + render(function.parameters().get(i)));
      }
      return "(" + String.join(", ", parameters) + ") => " + render(function.returnType());
    }
    throw new IllegalArgumentException("Cannot render TypeScript type " + type);
  }

  private static String renderParameters(List<String> names) {
    return names.isEmpty() ? "" : "<" + String.join(", ", names) + ">";
  }

  private static String renderTypeArguments(List<Type> types) {
    if (types.isEmpty()) {
      return "";
    }
    List<String> rendered = new ArrayList<>();
    for (Type type : types) {
      rendered.add(render(type));
    }
    return "<" + String.join(", ", rendered) + ">";
  }

  private static String renderArguments(List<Type> types) {
    if (types.isEmpty()) {
      return "()";
    }
    List<String> rendered = new ArrayList<>();
    for (int i = 0; i < types.size(); i++) {
      rendered.add("arg" + i + ": " + render(types.get(i)));
    }
    return "(" + String.join(", ", rendered) + ")";
  }

  private static Set<String> usedVariables(NativeMethod method) {
    Set<String> variables = new LinkedHashSet<>();
    collectVariables(method.returnType(), variables);
    for (Type parameterType : method.parameterTypes()) {
      collectVariables(parameterType, variables);
    }
    return variables;
  }

  private static void collectVariables(Type type, Set<String> variables) {
    if (type instanceof Type.Variable variable) {
      variables.add(variable.name());
    } else if (type instanceof Type.Nullable nullable) {
      collectVariables(nullable.inner(), variables);
    } else if (type instanceof Type.Nominal nominal) {
      for (Type argument : nominal.typeArguments()) {
        collectVariables(argument, variables);
      }
    } else if (type instanceof Type.Function function) {
      if (function.receiver() != null) {
        collectVariables(function.receiver(), variables);
      }
      for (Type parameter : function.parameters()) {
        collectVariables(parameter, variables);
      }
      collectVariables(function.returnType(), variables);
    }
  }
}
