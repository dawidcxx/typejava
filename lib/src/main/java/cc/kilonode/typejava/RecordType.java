package cc.kilonode.typejava;

import cc.kilonode.typejava.checker.Type;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RecordType {
  private final String name;
  private final Class<?> javaClass;
  private final List<Field> fields;

  private RecordType(String name, Class<?> javaClass, List<Field> fields) {
    this.name = name;
    this.javaClass = javaClass;
    this.fields = List.copyOf(fields);
  }

  public static RecordType create(Class<?> recordClass, Map<String, Class<?>> records) {
    if (!recordClass.isRecord()) {
      throw new IllegalArgumentException(
          "Bound type " + recordClass.getName() + " is not a record");
    }
    if (!isPublic(recordClass)) {
      throw new IllegalArgumentException(
          "Bound record " + recordClass.getName() + " must be public");
    }
    if (recordClass.getTypeParameters().length != 0) {
      throw new IllegalArgumentException(
          "Bound record " + recordClass.getName() + " must not declare type parameters");
    }

    String name = recordClass.getSimpleName();
    List<Field> fields = new ArrayList<>();
    for (RecordComponent component : recordClass.getRecordComponents()) {
      Class<?> javaType = component.getType();
      String descriptor = org.objectweb.asm.Type.getDescriptor(javaType);
      Type type = fieldType(name, component, javaType, records);
      fields.add(new Field(component.getName(), javaType, descriptor, type));
    }
    return new RecordType(name, recordClass, fields);
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

  public String descriptor() {
    return "L" + internalName() + ";";
  }

  public List<Field> fields() {
    return fields;
  }

  public Field field(String fieldName) {
    for (Field field : fields) {
      if (field.name().equals(fieldName)) {
        return field;
      }
    }
    return null;
  }

  public String constructorDescriptor() {
    StringBuilder descriptor = new StringBuilder("(");
    for (Field field : fields) {
      descriptor.append(field.descriptor());
    }
    return descriptor.append(")V").toString();
  }

  private static Type fieldType(
      String owner,
      RecordComponent component,
      Class<?> javaType,
      Map<String, Class<?>> records) {
    boolean maybeNull = component.getAnnotation(MaybeNull.class) != null;
    if (javaType == double.class) {
      requireNonNullable(maybeNull, owner, component);
      return Type.NUMBER;
    }
    if (javaType == Double.class) {
      return maybeNull ? Type.nullable(Type.NUMBER) : Type.NUMBER;
    }
    if (javaType == int.class) {
      requireNonNullable(maybeNull, owner, component);
      return Type.NUMBER;
    }
    if (javaType == boolean.class) {
      requireNonNullable(maybeNull, owner, component);
      return Type.BOOLEAN;
    }
    if (javaType == Boolean.class) {
      return maybeNull ? Type.nullable(Type.BOOLEAN) : Type.BOOLEAN;
    }
    if (javaType == String.class) {
      return maybeNull ? Type.nullable(Type.STRING) : Type.STRING;
    }
    if (javaType.isRecord()) {
      Class<?> bound = records.get(javaType.getSimpleName());
      if (bound != javaType) {
        throw new IllegalArgumentException(
            "Nested record "
                + javaType.getName()
                + " used by "
                + owner
                + "."
                + component.getName()
                + " is not bound; call record("
                + javaType.getSimpleName()
                + ".class)");
      }
      Type nominal = new Type.Nominal(javaType.getSimpleName(), List.of());
      return maybeNull ? Type.nullable(nominal) : nominal;
    }
    throw new IllegalArgumentException(
        "Unsupported record component type "
            + javaType.getName()
            + " on "
            + owner
            + "."
            + component.getName()
            + "; use double, int, boolean, String, or another bound record");
  }

  private static void requireNonNullable(
      boolean maybeNull, String owner, RecordComponent component) {
    if (maybeNull) {
      throw new IllegalArgumentException(
          "Primitive record component "
              + owner
              + "."
              + component.getName()
              + " cannot be annotated @MaybeNull");
    }
  }

  private static boolean isPublic(Class<?> type) {
    for (Class<?> current = type; current != null; current = current.getEnclosingClass()) {
      if (!Modifier.isPublic(current.getModifiers())) {
        return false;
      }
    }
    return true;
  }

  public record Field(String name, Class<?> javaType, String descriptor, Type type) {}
}
