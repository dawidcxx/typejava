package cc.kilonode.typejava.checker;

import java.util.List;

public final class ObjectTypeInfo {
  private final String name;
  private final List<String> typeParameters;
  private final List<FieldInfo> fields;

  public ObjectTypeInfo(String name, List<String> typeParameters, List<FieldInfo> fields) {
    this.name = name;
    this.typeParameters = List.copyOf(typeParameters);
    this.fields = List.copyOf(fields);
  }

  public String name() {
    return name;
  }

  public List<String> typeParameters() {
    return typeParameters;
  }

  public List<FieldInfo> fields() {
    return fields;
  }

  public boolean hasMethods() {
    return fields.stream().anyMatch(FieldInfo::isMethod);
  }

  public Type.Nominal nominalType(List<Type> arguments) {
    return new Type.Nominal(name, arguments);
  }

  public Type fieldType(FieldInfo field, List<Type> arguments) {
    if (arguments.size() != typeParameters.size()) {
      throw new IllegalArgumentException("Wrong number of type arguments for " + name);
    }
    java.util.Map<String, Type> substitutions = new java.util.LinkedHashMap<>();
    for (int i = 0; i < typeParameters.size(); i++) {
      substitutions.put(typeParameters.get(i), arguments.get(i));
    }
    return Types.substitute(field.type(), substitutions);
  }
}
