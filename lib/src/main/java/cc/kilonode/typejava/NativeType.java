package cc.kilonode.typejava;

import cc.kilonode.typejava.checker.Type;
import java.util.ArrayList;
import java.util.List;

public final class NativeType {
  private static final String ENVIRONMENT_DESCRIPTOR =
      "L" + Environment.class.getName().replace('.', '/') + ";";

  private final String name;
  private final Class<?> javaClass;
  private final List<String> parameterNames = new ArrayList<>();
  private final List<MethodSpec> specs = new ArrayList<>();
  private List<NativeMethod> methods;
  private boolean resolved;

  private NativeType(String name, Class<?> javaClass) {
    this.name = name;
    this.javaClass = javaClass;
  }

  public static NativeType of(String name, Class<?> javaClass) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Native type name must not be blank");
    }
    if (!isIdentifier(name)) {
      throw new IllegalArgumentException("Native type name is not a valid identifier: " + name);
    }
    if (javaClass == null) {
      throw new IllegalArgumentException("Native type class must not be null");
    }
    return new NativeType(name, javaClass);
  }

  public NativeType typeParameters(String... names) {
    ensureMutable();
    for (String parameter : names) {
      if (!isIdentifier(parameter)) {
        throw new IllegalArgumentException(
            "Native type parameter is not a valid identifier: " + parameter);
      }
      if (parameterNames.contains(parameter)) {
        throw new IllegalArgumentException(
            "Duplicate native type parameter '" + parameter + "' on " + name);
      }
      parameterNames.add(parameter);
    }
    methods = null;
    return this;
  }

  public NativeType factory(String name, String descriptor, String returnType) {
    return addMethod(name, descriptor, returnType, List.of(), true, true);
  }

  public NativeType staticMethod(
      String name, String descriptor, String returnType, String... parameterTypes) {
    return addMethod(name, descriptor, returnType, List.of(parameterTypes), true, false);
  }

  public NativeType instanceMethod(
      String name, String descriptor, String returnType, String... parameterTypes) {
    return addMethod(name, descriptor, returnType, List.of(parameterTypes), false, false);
  }

  public String name() {
    return name;
  }

  public Class<?> javaClass() {
    return javaClass;
  }

  public String internalName() {
    return javaClass.getName().replace('.', '/');
  }

  public List<String> parameterNames() {
    return List.copyOf(parameterNames);
  }

  public List<NativeMethod> methods() {
    resolve();
    return methods;
  }

  public NativeMethod method(String name) {
    resolve();
    for (NativeMethod method : methods) {
      if (method.name().equals(name)) {
        return method;
      }
    }
    return null;
  }

  NativeType resolve() {
    if (resolved) {
      return this;
    }
    List<NativeMethod> resolvedMethods = new ArrayList<>();
    for (MethodSpec spec : specs) {
      Type returnType = parseType(spec.returnType());
      List<Type> parameterTypes = new ArrayList<>();
      for (String parameterType : spec.parameterTypes()) {
        parameterTypes.add(parseType(parameterType));
      }
      resolvedMethods.add(
          new NativeMethod(
              spec.name(),
              spec.descriptor(),
              returnType,
              parameterTypes,
              spec.isStatic(),
              spec.isFactory(),
              spec.descriptor().startsWith("(" + ENVIRONMENT_DESCRIPTOR)));
    }
    methods = List.copyOf(resolvedMethods);
    resolved = true;
    return this;
  }

  private NativeType addMethod(
      String methodName,
      String descriptor,
      String returnType,
      List<String> parameterTypes,
      boolean isStatic,
      boolean isFactory) {
    ensureMutable();
    if (methodName == null || !isIdentifier(methodName)) {
      throw new IllegalArgumentException(
          "Native method name is not a valid identifier: " + methodName);
    }
    if (descriptor == null || !descriptor.startsWith("(") || !descriptor.contains(")")) {
      throw new IllegalArgumentException(
          "Invalid JVM descriptor for " + name + "." + methodName + ": " + descriptor);
    }
    for (MethodSpec spec : specs) {
      if (spec.name().equals(methodName)) {
        throw new IllegalArgumentException(
            "Duplicate native method '" + methodName + "' on " + name);
      }
    }
    specs.add(
        new MethodSpec(
            methodName, descriptor, returnType, List.copyOf(parameterTypes), isStatic, isFactory));
    methods = null;
    return this;
  }

  private Type parseType(String spec) {
    if (spec == null || spec.isBlank()) {
      throw new IllegalArgumentException("Native type reference must not be blank on " + name);
    }
    String text = spec.trim();
    int open = text.indexOf('<');
    if (open < 0) {
      return switch (text) {
        case "number" -> Type.NUMBER;
        case "string" -> Type.STRING;
        case "boolean" -> Type.BOOLEAN;
        case "void" -> Type.VOID;
        case "null" -> Type.NULL;
        default -> parseNamed(text, List.of());
      };
    }
    if (!text.endsWith(">")) {
      throw new IllegalArgumentException(
          "Invalid native type reference '" + spec + "' on " + name);
    }
    String base = text.substring(0, open).trim();
    if (base.isEmpty()) {
      throw new IllegalArgumentException(
          "Invalid native type reference '" + spec + "' on " + name);
    }
    List<String> parts = splitTypeArguments(text.substring(open + 1, text.length() - 1));
    List<Type> arguments = new ArrayList<>();
    for (String part : parts) {
      arguments.add(parseType(part));
    }
    return parseNamed(base, arguments);
  }

  private Type parseNamed(String name, List<Type> arguments) {
    if (parameterNames.contains(name)) {
      if (!arguments.isEmpty()) {
        throw new IllegalArgumentException(
            "Type parameter '" + name + "' cannot take type arguments on " + this.name);
      }
      return new Type.Variable(name);
    }
    return new Type.Nominal(name, arguments);
  }

  private static List<String> splitTypeArguments(String text) {
    List<String> parts = new ArrayList<>();
    int depth = 0;
    int start = 0;
    for (int i = 0; i < text.length(); i++) {
      char character = text.charAt(i);
      if (character == '<') {
        depth++;
      } else if (character == '>') {
        depth--;
      } else if (character == ',' && depth == 0) {
        parts.add(text.substring(start, i).trim());
        start = i + 1;
      }
    }
    parts.add(text.substring(start).trim());
    return parts;
  }

  private void ensureMutable() {
    if (resolved) {
      throw new IllegalStateException("Native type '" + name + "' is already bound");
    }
  }

  private static boolean isIdentifier(String text) {
    if (text == null || text.isEmpty() || !Character.isJavaIdentifierStart(text.charAt(0))) {
      return false;
    }
    for (int i = 1; i < text.length(); i++) {
      if (!Character.isJavaIdentifierPart(text.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private record MethodSpec(
      String name,
      String descriptor,
      String returnType,
      List<String> parameterTypes,
      boolean isStatic,
      boolean isFactory) {}
}
