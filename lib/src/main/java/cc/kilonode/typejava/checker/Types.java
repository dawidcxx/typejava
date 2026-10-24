package cc.kilonode.typejava.checker;

public final class Types {
  private Types() {}

  public static Type stripNullable(Type type) {
    return type instanceof Type.Nullable nullable ? nullable.inner() : type;
  }

  public static boolean isNullable(Type type) {
    return type instanceof Type.Nullable;
  }

  public static boolean isNumeric(Type type) {
    return Type.NUMBER.equals(type);
  }

  public static boolean isReference(Type type) {
    return type instanceof Type.Nominal
        || type instanceof Type.Nullable
        || type instanceof Type.Null
        || type instanceof Type.Variable
        || Type.STRING.equals(type);
  }

  public static boolean isVoid(Type type) {
    return Type.VOID.equals(type);
  }

  public static Type substitute(Type type, java.util.Map<String, Type> substitutions) {
    if (substitutions.isEmpty()) {
      return type;
    }
    if (type instanceof Type.Variable variable) {
      return substitutions.getOrDefault(variable.name(), variable);
    }
    if (type instanceof Type.Nullable nullable) {
      return new Type.Nullable(substitute(nullable.inner(), substitutions));
    }
    if (type instanceof Type.Nominal nominal) {
      return new Type.Nominal(
          nominal.name(),
          nominal.typeArguments().stream()
              .map(argument -> substitute(argument, substitutions))
              .toList());
    }
    if (type instanceof Type.Function function) {
      return new Type.Function(
          function.receiver() == null ? null : substitute(function.receiver(), substitutions),
          function.parameters().stream()
              .map(parameter -> substitute(parameter, substitutions))
              .toList(),
          substitute(function.returnType(), substitutions));
    }
    return type;
  }

  public static boolean isAssignable(Type target, Type source) {
    if (target.equals(source)) {
      return true;
    }
    if (target instanceof Type.Nullable nullable) {
      if (source instanceof Type.Null) {
        return true;
      }
      return isAssignable(nullable.inner(), source);
    }
    if (source instanceof Type.Null) {
      return target instanceof Type.Nullable;
    }
    if (target instanceof Type.Function targetFunction
        && source instanceof Type.Function sourceFunction) {
      return functionAssignable(targetFunction, sourceFunction);
    }
    return false;
  }

  private static boolean functionAssignable(Type.Function target, Type.Function source) {
    if (target.receiver() == null) {
      if (source.receiver() != null) {
        return false;
      }
    } else if (source.receiver() == null || !target.receiver().equals(source.receiver())) {
      return false;
    }
    if (target.parameters().size() != source.parameters().size()) {
      return false;
    }
    for (int i = 0; i < target.parameters().size(); i++) {
      if (!isAssignable(source.parameters().get(i), target.parameters().get(i))) {
        return false;
      }
    }
    return isAssignable(target.returnType(), source.returnType());
  }

  public static String display(Type type) {
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
      if (nominal.typeArguments().isEmpty()) {
        return nominal.name();
      }
      return nominal.name()
          + "<"
          + String.join(", ", nominal.typeArguments().stream().map(Types::display).toList())
          + ">";
    }
    if (type instanceof Type.Function function) {
      String parameters =
          String.join(", ", function.parameters().stream().map(Types::display).toList());
      String receiver =
          function.receiver() == null ? "" : "this: " + display(function.receiver()) + ", ";
      return "(" + receiver + parameters + ") => " + display(function.returnType());
    }
    if (type instanceof Type.Nullable nullable) {
      return display(nullable.inner()) + " | null";
    }
    if (type instanceof Type.Variable variable) {
      return variable.name();
    }
    return type.toString();
  }
}
