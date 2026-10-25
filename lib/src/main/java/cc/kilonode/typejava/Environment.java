package cc.kilonode.typejava;

import cc.kilonode.typejava.runtime.TjArray;
import cc.kilonode.typejava.runtime.TjMap;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Environment {
  private static final Set<String> RESERVED_NAMES =
      Set.of("number", "string", "boolean", "void", "null", "console");

  private final Io io;
  private final Map<String, NativeType> types;
  private final Map<String, RecordType> records;
  private final List<Class<?>> exportInterfaces;

  private Environment(
      Io io,
      Map<String, NativeType> types,
      Map<String, RecordType> records,
      List<Class<?>> exportInterfaces) {
    this.io = io;
    this.types = types;
    this.records = records;
    this.exportInterfaces = exportInterfaces;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Environment standard() {
    return builder().withStd().build();
  }

  public Io io() {
    return io;
  }

  public Optional<NativeType> findType(String name) {
    return Optional.ofNullable(types.get(name));
  }

  public Collection<NativeType> types() {
    return types.values();
  }

  public Optional<RecordType> findRecord(String name) {
    return Optional.ofNullable(records.get(name));
  }

  public Collection<RecordType> records() {
    return records.values();
  }

  public List<Class<?>> exportInterfaces() {
    return exportInterfaces;
  }

  /**
   * Generates ambient TypeScript declarations for the native types, bound records, and export
   * contracts in this environment.
   *
   * @return TypeScript declaration file contents
   */
  public String typeScriptDeclarations() {
    return TypeScriptDeclarations.generate(this);
  }

  public static final class Builder {
    private final Map<String, NativeType> types = new LinkedHashMap<>();
    private final Map<String, Class<?>> recordClasses = new LinkedHashMap<>();
    private final List<Class<?>> exportInterfaces = new ArrayList<>();
    private Io io;

    public Builder io(Io io) {
      this.io = Objects.requireNonNull(io, "io");
      return this;
    }

    public Builder withStd() {
      return withArray().withMap();
    }

    public Builder withArray() {
      return bind(standardArray());
    }

    public Builder withMap() {
      return bind(standardMap());
    }

    public Builder bind(NativeType type) {
      Objects.requireNonNull(type, "type");
      type.resolve();
      if (RESERVED_NAMES.contains(type.name())) {
        throw new IllegalArgumentException(
            "Cannot bind native type '" + type.name() + "': name is reserved");
      }
      NativeType existing = types.putIfAbsent(type.name(), type);
      if (existing != null) {
        throw new IllegalArgumentException(
            "Native type '" + type.name() + "' is already bound");
      }
      return this;
    }

    public Builder record(Class<?> recordClass) {
      Objects.requireNonNull(recordClass, "recordClass");
      if (!recordClass.isRecord()) {
        throw new IllegalArgumentException(
            "Bound type " + recordClass.getName() + " is not a record");
      }
      String name = recordClass.getSimpleName();
      if (RESERVED_NAMES.contains(name)) {
        throw new IllegalArgumentException(
            "Cannot bind record '" + recordClass.getName() + "': name is reserved");
      }
      if (types.containsKey(name)) {
        throw new IllegalArgumentException(
            "Record '" + name + "' conflicts with a native type of the same name");
      }
      Class<?> existing = recordClasses.putIfAbsent(name, recordClass);
      if (existing != null && existing != recordClass) {
        throw new IllegalArgumentException(
            "Record '" + name + "' is already bound to " + existing.getName());
      }
      return this;
    }

    public Builder exports(Class<?>... interfaces) {
      Objects.requireNonNull(interfaces, "interfaces");
      for (Class<?> api : interfaces) {
        Objects.requireNonNull(api, "export interface");
        if (!api.isInterface()) {
          throw new IllegalArgumentException(
              "Export contract " + api.getName() + " is not an interface");
        }
        if (!Modifier.isPublic(api.getModifiers())) {
          throw new IllegalArgumentException(
              "Export contract " + api.getName() + " must be public");
        }
        if (!exportInterfaces.contains(api)) {
          exportInterfaces.add(api);
        }
      }
      return this;
    }

    public Environment build() {
      Map<String, RecordType> records = new LinkedHashMap<>();
      Map<String, Class<?>> immutableRecords = Map.copyOf(recordClasses);
      for (Class<?> recordClass : recordClasses.values()) {
        String name = recordClass.getSimpleName();
        if (types.containsKey(name)) {
          throw new IllegalArgumentException(
              "Record '" + name + "' conflicts with a native type of the same name");
        }
        records.put(name, RecordType.create(recordClass, immutableRecords));
      }
      return new Environment(
          io == null ? new DefaultIo() : io,
          Map.copyOf(types),
          Map.copyOf(records),
          List.copyOf(exportInterfaces));
    }
  }

  private static NativeType standardArray() {
    return NativeType.of("TjArray", TjArray.class)
        .typeParameters("T")
        .factory("create", "()Lcc/kilonode/typejava/runtime/TjArray;", "TjArray<T>")
        .instanceMethod("size", "()I", "number")
        .instanceMethod("get", "(I)Ljava/lang/Object;", "T", "number")
        .instanceMethod("add", "(Ljava/lang/Object;)V", "void", "T");
  }

  private static NativeType standardMap() {
    return NativeType.of("TjMap", TjMap.class)
        .typeParameters("K", "V")
        .factory("create", "()Lcc/kilonode/typejava/runtime/TjMap;", "TjMap<K, V>")
        .instanceMethod("size", "()I", "number")
        .instanceMethod("get", "(Ljava/lang/Object;)Ljava/lang/Object;", "V", "K")
        .instanceMethod("put", "(Ljava/lang/Object;Ljava/lang/Object;)V", "void", "K", "V")
        .instanceMethod("containsKey", "(Ljava/lang/Object;)Z", "boolean", "K");
  }
}
