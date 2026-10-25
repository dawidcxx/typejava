package cc.kilonode.typejava;

import cc.kilonode.typejava.checker.Type;
import cc.kilonode.typejava.checker.Types;

public final class HostTypes {
  private HostTypes() {}

  public static Class<?> hostClass(Type type, Environment environment) {
    if (Type.NUMBER.equals(type)) {
      return double.class;
    }
    if (Type.STRING.equals(type)) {
      return String.class;
    }
    if (Type.BOOLEAN.equals(type)) {
      return boolean.class;
    }
    if (Type.VOID.equals(type)) {
      return void.class;
    }
    if (type instanceof Type.Nullable nullable) {
      Type inner = nullable.inner();
      if (Type.NUMBER.equals(inner)) {
        return Double.class;
      }
      if (Type.BOOLEAN.equals(inner)) {
        return Boolean.class;
      }
      if (Type.STRING.equals(inner)) {
        return String.class;
      }
      if (inner instanceof Type.Nominal) {
        return hostClass(inner, environment);
      }
      throw unsupported(type);
    }
    if (type instanceof Type.Nominal nominal) {
      RecordType record = environment.findRecord(nominal.name()).orElse(null);
      if (record != null) {
        return record.javaClass();
      }
      NativeType nativeType = environment.findType(nominal.name()).orElse(null);
      if (nativeType != null) {
        return nativeType.javaClass();
      }
      throw unsupported(type);
    }
    throw unsupported(type);
  }

  public static boolean isHostBound(Type type, Environment environment) {
    try {
      hostClass(type, environment);
      return true;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private static IllegalArgumentException unsupported(Type type) {
    return new IllegalArgumentException(
        "Type "
            + Types.display(type)
            + " cannot cross an export boundary; bind a record or use a native type");
  }
}
